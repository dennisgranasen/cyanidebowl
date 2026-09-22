#!/usr/bin/env python3
"""Refresh the versioned BBBase star player catalogue (explicit network operation)."""
import argparse
import concurrent.futures
import json
import pathlib
import re
import urllib.parse
import urllib.request

REPO = 'BBBase-EHeresy-PPPub/BBBase'


def fetch(url):
    with urllib.request.urlopen(url, timeout=30) as response:
        return response.read().decode('utf-8')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--ref', default='main', help='BBBase commit or branch')
    args = parser.parse_args()
    tree = json.loads(fetch(f'https://api.github.com/repos/{REPO}/git/trees/{urllib.parse.quote(args.ref, safe="")}?recursive=1'))
    if tree.get('truncated'):
        raise RuntimeError('GitHub returned a truncated file tree')
    commit = tree['sha']
    base = f'https://raw.githubusercontent.com/{REPO}/{commit}/'
    paths = [entry['path'] for entry in tree['tree']
             if entry['path'].startswith('docs/bb2025/starplayers/') and entry['path'].endswith('.md')]

    def read(path):
        markdown = fetch(base + urllib.parse.quote(path))
        heading = re.search(r'^# (.+)', markdown, re.M)
        if not heading:
            return None  # Non-player include/index files.
        portrait = re.search(r'!\[[^\]]*\]\(\.\./media/starplayers/([^)]*)\)', markdown)
        if not portrait:
            raise RuntimeError(f'Missing portrait reference in {path}')
        image_path = 'docs/bb2025/media/starplayers/' + portrait.group(1)
        if not any(entry['path'] == image_path for entry in tree['tree']):
            raise RuntimeError(f'Missing portrait: {image_path}')
        return dict(id=path.rsplit('/', 1)[1][:-3], name=heading.group(1).strip(),
                    markdown=markdown, sourceUrl=f'https://github.com/{REPO}/blob/{commit}/' + urllib.parse.quote(path),
                    imageUrl=base + urllib.parse.quote(image_path))

    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        players = [player for player in pool.map(read, paths) if player]
    if not players:
        raise RuntimeError('No star players found; refusing to replace catalogue')
    output = pathlib.Path(__file__).resolve().parents[1] / 'backend/src/main/resources/ai/starplayers.json'
    output.write_text(json.dumps(dict(sourceCommit=commit, players=players), ensure_ascii=False, indent=2) + '\n')
    print(f'Imported {len(players)} star players from {commit}')


if __name__ == '__main__':
    main()
