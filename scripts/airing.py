#!/usr/bin/env python3
"""
Script to scrape currently airing episodes from AnimePahe and save to daily_update.json
"""

import asyncio
import re
import json
import logging
import os
from datetime import datetime
from playwright.async_api import async_playwright

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class AiringEpisodesScraper:
    def __init__(self):
        self.base_url = "https://animepahe.si"
        # Changed output file to daily_update.json as per prompt requirements
        self.output_file = 'daily_update.json'
        
    async def scrape_airing_episodes(self, pages=5):
        """Scrape currently airing episodes from multiple pages"""
        playwright = await async_playwright().start()
        browser = await playwright.chromium.launch(
            headless=True,
            args=[
                '--disable-blink-features=AutomationControlled',
                '--no-sandbox',
                '--disable-dev-shm-usage'
            ]
        )
        
        context = await browser.new_context(
            viewport={'width': 1920, 'height': 1080},
            user_agent='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        )
        
        page = await context.new_page()
        all_episodes = []
        
        try:
            for page_num in range(1, pages + 1):
                if page_num == 1:
                    url = self.base_url
                else:
                    url = f"{self.base_url}?page={page_num}"
                
                logger.info(f"📺 Scraping page {page_num}: {url}")
                
                await page.goto(url, wait_until='networkidle', timeout=60000)
                await page.wait_for_timeout(5000)
                
                # Handle DDoS-Guard
                page_title = await page.title()
                if 'DDoS-Guard' in page_title or 'Just a moment' in page_title:
                    logger.info("🛡️ DDoS-Guard detected, waiting...")
                    await page.wait_for_timeout(10000)
                    await page.reload(wait_until='networkidle')
                    await page.wait_for_timeout(5000)
                
                # Wait for content to load
                try:
                    await page.wait_for_selector('a[href*="/play/"]', timeout=15000)
                except Exception:
                    logger.warning(f"⚠️ No episode links found on page {page_num}")
                    continue
                
                # More specific selectors to avoid duplicates
                episode_selectors = [
                    '.episode-list .episode-item a[href*="/play/"]',  # Specific episode items
                    '.tab-content .episode-item a[href*="/play/"]',   # Tab content episodes
                    'a.episode-link[href*="/play/"]',                 # Episode links
                    '.main-content a[href*="/play/"]',                # Main content area
                ]
                
                episode_links = []
                for selector in episode_selectors:
                    try:
                        links = await page.query_selector_all(selector)
                        if links:
                            episode_links.extend(links)
                            logger.info(f"🎯 Found {len(links)} episode links with selector: {selector}")
                            # If we found good links, use them
                            if len(links) >= 5:
                                break
                    except Exception as e:
                        logger.warning(f"⚠️ Selector {selector} failed: {e}")
                        continue
                
                # Fallback to general play links if specific selectors didn't work
                if not episode_links:
                    episode_links = await page.query_selector_all('a[href*="/play/"]')
                    logger.info(f"🎯 Found {len(episode_links)} episode links via fallback")
                
                # Remove duplicate links by href
                unique_links = []
                seen_hrefs = set()
                for link in episode_links:
                    href = await link.get_attribute('href')
                    if href and href not in seen_hrefs:
                        seen_hrefs.add(href)
                        unique_links.append(link)
                
                logger.info(f"🎯 Processing {len(unique_links)} unique episode links on page {page_num}")
                
                # Extract information from each unique episode link
                episode_count = 0
                for link in unique_links:
                    try:
                        episode_data = await self.extract_episode_info_from_link(link)
                        if episode_data:
                            # Check if we already have this episode (by session_id)
                            existing_episodes = [ep for ep in all_episodes if ep['session_id'] == episode_data['session_id']]
                            if not existing_episodes:
                                all_episodes.append(episode_data)
                                episode_count += 1
                                logger.info(f"✅ Extracted: {episode_data['anime_name']} - Episode {episode_data['episode_number']}")
                    except Exception as e:
                        logger.warning(f"⚠️ Error processing episode link: {e}")
                        continue
                
                logger.info(f"📊 Page {page_num}: Added {episode_count} new episodes")
                
                # Stop if we have enough unique episodes
                if len(all_episodes) >= 100:  # Increased limit for 5 pages
                    logger.info(f"🎯 Reached episode limit of 100, stopping early")
                    break
                    
        except Exception as e:
            logger.error(f"❌ Error scraping page: {e}")
        finally:
            await context.close()
            await browser.close()
            await playwright.stop()
        
        logger.info(f"📊 Total unique episodes collected: {len(all_episodes)}")
        return all_episodes
    
    async def extract_episode_info_from_link(self, link):
        """Extract episode information directly from a link element"""
        try:
            href = await link.get_attribute('href')
            if not href:
                return None
            
            # Extract anime ID and session ID from URL
            url_match = re.search(r'/play/([a-f0-9-]+)/([a-f0-9]+)', href)
            if not url_match:
                return None
            
            anime_id = url_match.group(1)
            session_id = url_match.group(2)
            
            # Get the text content of the link and its parent for context
            link_text = await link.text_content()
            parent = await link.query_selector('xpath=..')  # Get parent element
            parent_text = await parent.text_content() if parent else link_text
            
            if not parent_text:
                return None
            
            # Parse anime name and episode number
            anime_name, episode_number = self.parse_episode_text(parent_text.strip())
            
            return {
                'anime_name': anime_name,
                'episode_number': episode_number,
                'episode_title': f"{anime_name} - Episode {episode_number}",
                'anime_id': anime_id,
                'session_id': session_id,
                'episode_url': f"{self.base_url}/play/{anime_id}/{session_id}"
            }
            
        except Exception as e:
            logger.warning(f"⚠️ Error extracting episode info from link: {e}")
            return None
    
    def parse_episode_text(self, text):
        """Parse anime name and episode number from text with improved logic"""
        if not text:
            return "Unknown Anime", "1"
        
        # Clean the text - remove extra whitespace and common prefixes
        clean_text = re.sub(r'\s+', ' ', text).strip()
        clean_text = re.sub(r'^Watch\s+', '', clean_text)
        clean_text = re.sub(r'\s+Online\s*$', '', clean_text)
        clean_text = re.sub(r'\bBD\b', '', clean_text)  # Remove BD text
        
        # Extract the main anime name and episode number
        patterns = [
            r'^(.+?)\s*-\s*[Ee]pisode\s*(\d+).*$',
            r'^(.+?)\s*-\s*[Ee][Pp]\s*(\d+).*$',
            r'^(.+?)\s+[Ee]pisode\s*(\d+).*$',
            r'^(.+?)\s+(\d+)\s*$',
            r'^(.+?)\s*-\s*(\d+)\s*$',
        ]
        
        anime_name = "Unknown Anime"
        episode_number = "1"
        
        for pattern in patterns:
            match = re.search(pattern, clean_text)
            if match and len(match.groups()) >= 2:
                potential_name = match.group(1).strip()
                potential_number = match.group(2).strip()
                
                if potential_number.isdigit():
                    episode_number = str(int(potential_number)) # Keep as string for consistency with app models if needed, but JSON usually int is fine. App expects String in Model.
                    anime_name = potential_name
                    
                    # Clean up the anime name
                    anime_name = re.sub(r'\s*-\s*$', '', anime_name)
                    anime_name = re.sub(r'\s+$', '', anime_name)
                    
                    if len(anime_name) >= 2 and not anime_name.isdigit():
                        break
        
        # If no good pattern found, try to extract numbers
        if anime_name == "Unknown Anime":
            numbers = re.findall(r'\b(\d+)\b', clean_text)
            if numbers:
                # Assuming the last number is likely the episode number if name failed
                episode_number = str(max(map(int, numbers)))
                # Try to extract anime name by removing the number and common suffixes
                anime_name = re.sub(r'\s*\b\d+\b.*$', '', clean_text).strip()
                anime_name = re.sub(r'\s*-\s*$', '', anime_name)
        
        # Final cleanup
        anime_name = re.sub(r'^\s*-\s*', '', anime_name)
        anime_name = re.sub(r'\s*-\s*$', '', anime_name)
        anime_name = anime_name.strip()
        
        if not anime_name or anime_name.isdigit() or len(anime_name) < 2:
            anime_name = "Unknown Anime"
        
        return anime_name, str(episode_number)
    
    def save_to_file(self, episodes):
        """Save episodes to daily_update.json"""
        try:
            data = {
                'timestamp': datetime.now().isoformat(),
                'count': len(episodes),
                'episodes': episodes
            }
            
            # Save to daily_update.json in the root directory (parent of scripts/)
            # The script is in scripts/ so we go up one level
            root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
            output_path = os.path.join(root_dir, self.output_file)
            
            with open(output_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
            
            logger.info(f"💾 Saved {len(episodes)} episodes to: {output_path}")
            return True
            
        except Exception as e:
            logger.error(f"❌ Error saving to file: {e}")
            return False

async def main():
    """Main function to run the scraper"""
    logger.info("🚀 Starting AnimePahe Airing Episodes Scraper - 5 Pages")
    
    scraper = AiringEpisodesScraper()
    
    try:
        # Scrape episodes from 5 pages
        episodes = await scraper.scrape_airing_episodes(pages=5)
        
        # Save to file
        success = scraper.save_to_file(episodes)
        
        if success:
            logger.info(f"✅ Successfully updated daily_update.json with {len(episodes)} episodes")
        else:
            logger.error("❌ Failed to save episodes")
            
    except Exception as e:
        logger.error(f"❌ Scraping failed: {e}")

if __name__ == '__main__':
    asyncio.run(main())
