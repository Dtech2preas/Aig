
import json
import glob
import os

# Paths
ANIME_INDEX_DIR = '/tmp/file_attachments/DTECHANIME/anime_index'
OUTPUT_FILE = 'android-app/app/src/main/assets/www/data/popular.json'

def generate_popular():
    all_anime = []
    # Load all anime
    json_files = glob.glob(os.path.join(ANIME_INDEX_DIR, 'anime_*.json'))
    print(f"Found {len(json_files)} json files in {ANIME_INDEX_DIR}")

    for json_file in json_files:
        try:
            with open(json_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
                if 'anime' in data:
                    all_anime.extend(data['anime'])
        except Exception as e:
            print(f"Error loading {json_file}: {e}")

    print(f"Total anime loaded: {len(all_anime)}")

    popular_titles = [
        "Jujutsu Kaisen", "One Piece", "Dan Da Dan", "Kaiju No. 8", "Black Clover",
        "Demon Slayer", "Akame ga Kill", "Chainsaw Man", "Naruto", "Bleach",
        "Eminence in Shadow", "Attack on Titan", "My Hero Academia", "Spy x Family",
        "Tokyo Revengers", "Dr. Stone", "Blue Lock", "Haikyuu", "One Punch Man",
        "Mob Psycho 100", "Hunter x Hunter", "Death Note", "Fullmetal Alchemist: Brotherhood",
        "Code Geass", "Steins;Gate", "Re:Zero", "Konosuba", "Overlord"
    ]

    found_anime = []
    seen_ids = set()

    for title in popular_titles:
        for anime in all_anime:
            anime_title = anime.get('title', '').lower()
            search_title = title.lower()

            if (search_title == anime_title or
                search_title in anime_title or
                anime_title.startswith(search_title)):

                anime_id = anime.get('id')
                if anime_id and anime_id not in seen_ids:
                    seen_ids.add(anime_id)
                    found_anime.append({
                        'title': anime.get('title'),
                        'id': anime_id,
                        # We don't really need the URL for the app logic as it constructs it,
                        # but keeping it for consistency if needed.
                        'url': f"https://animepahe.si/anime/{anime_id}",
                        'poster': anime.get('poster') # Assuming poster might be there? pop.py didn't use it but index has it likely.
                    })
                    break

    # Write to file
    os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        json.dump(found_anime, f, indent=2)

    print(f"Written {len(found_anime)} popular anime to {OUTPUT_FILE}")

if __name__ == "__main__":
    generate_popular()
