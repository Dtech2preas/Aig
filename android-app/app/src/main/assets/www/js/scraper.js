
console.log("D-TECH Scraper: Loaded");

function scrapeFreshEpisodes() {
    console.log("D-TECH Scraper: Starting scrape...");

    // Check for DDoS-Guard or loading screens
    if (document.title.includes("DDoS-Guard") || document.title.includes("Just a moment")) {
        console.log("D-TECH Scraper: DDoS Guard detected");
        return { status: "WAIT", message: "DDoS Guard detected" };
    }

    // Selectors from airing.py
    const selectors = [
        '.episode-list .episode-item a[href*="/play/"]',
        '.tab-content .episode-item a[href*="/play/"]',
        'a.episode-link[href*="/play/"]',
        '.main-content a[href*="/play/"]',
        'a[href*="/play/"]' // Fallback
    ];

    let episodeLinks = [];
    for (const selector of selectors) {
        const links = document.querySelectorAll(selector);
        if (links.length > 0) {
            console.log(`D-TECH Scraper: Found ${links.length} links with selector: ${selector}`);
            episodeLinks = Array.from(links);
            break;
        }
    }

    if (episodeLinks.length === 0) {
        console.log("D-TECH Scraper: No episodes found");
        return { status: "RETRY", message: "No episodes found yet" };
    }

    const uniqueEpisodes = [];
    const seenSessionIds = new Set();
    const episodes = [];

    for (const link of episodeLinks) {
        try {
            const href = link.getAttribute('href');
            if (!href) continue;

            // Extract anime ID and session ID
            // URL format: /play/<anime_id>/<session_id>
            const match = href.match(/\/play\/([a-f0-9-]+)\/([a-f0-9]+)/);
            if (!match) continue;

            const animeId = match[1];
            const sessionId = match[2];

            if (seenSessionIds.has(sessionId)) continue;
            seenSessionIds.add(sessionId);

            // Extract text for name and episode number
            // Logic from parsing text in python script is quite complex regex.
            // We will attempt to get the most relevant text.
            let text = link.innerText;
            const parent = link.parentElement;
            if (parent) {
                text = parent.innerText; // Often the parent contains the full text "Anime Name - Episode X"
            }
            text = text.trim();

            const parsed = parseEpisodeText(text);

            episodes.push({
                anime_name: parsed.name,
                episode_number: parsed.number,
                episode_title: `${parsed.name} - Episode ${parsed.number}`,
                anime_id: animeId,
                session_id: sessionId,
                url: href
            });

        } catch (e) {
            console.error("D-TECH Scraper: Error processing link", e);
        }
    }

    console.log(`D-TECH Scraper: Found ${episodes.length} unique episodes`);
    return { status: "SUCCESS", data: episodes };
}

function parseEpisodeText(text) {
    if (!text) return { name: "Unknown Anime", number: 1 };

    // Clean text
    let cleanText = text.replace(/\s+/g, ' ').trim();
    cleanText = cleanText.replace(/^Watch\s+/, '').replace(/\s+Online\s*$/, '').replace(/\bBD\b/, '');

    let name = "Unknown Anime";
    let number = 1;

    // Regex patterns similar to python script
    const patterns = [
        /^(.+?)\s*-\s*[Ee]pisode\s*(\d+).*$/,
        /^(.+?)\s*-\s*[Ee][Pp]\s*(\d+).*$/,
        /^(.+?)\s+[Ee]pisode\s*(\d+).*$/,
        /^(.+?)\s+(\d+)\s*$/,
        /^(.+?)\s*-\s*(\d+)\s*$/
    ];

    for (const pattern of patterns) {
        const match = cleanText.match(pattern);
        if (match) {
            const potentialName = match[1].trim();
            const potentialNumber = match[2].trim();

            if (!isNaN(potentialNumber)) {
                number = parseInt(potentialNumber, 10);
                name = potentialName.replace(/\s*-\s*$/, '').replace(/\s+$/, '');
                if (name.length >= 2 && isNaN(name)) {
                    return { name, number };
                }
            }
        }
    }

    // Fallback: extract last number
    const numbers = cleanText.match(/\b(\d+)\b/g);
    if (numbers) {
        number = parseInt(numbers[numbers.length - 1], 10);
        name = cleanText.replace(new RegExp(`\\s*\\b${number}\\b.*$`), '').trim();
        name = name.replace(/\s*-\s*$/, '');
    }

    if (!name || !isNaN(name) || name.length < 2) name = "Unknown Anime";

    return { name, number };
}
