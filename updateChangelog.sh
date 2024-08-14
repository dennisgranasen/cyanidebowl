#!/bin/bash
#
# Updates a CHANGELOG.md or changelog.md with the current version
#
# requires a CHANGELOG.md or changelog.md in the current path
# requires the currently released version as first argument, e.g. "$POM_BASE_VERSION" in maven projects
#
# Replaces the Headline '## Unreleased' in the CHANGELOG.md with the given version and the date of today
# Adds a new headline '## Unreleased' with a placeholder text 'No changes yet.'
#
# After execution the modification has to be committed
#

CHANGELOG_VERSION="$1"
TODAY=$(date "+%Y-%m-%d")
if [ "$CHANGELOG_VERSION" == "" ]; then
  echo "No version given. Skipping update."
  exit 0;
fi

CHANGELOG_FILE=""
if [ -f "./CHANGELOG.md" ]; then
  CHANGELOG_FILE="./CHANGELOG.md"
elif [ -f "./changelog.md" ]; then
  CHANGELOG_FILE="./changelog.md"
else
  echo "CHANGELOG.md does not exits. Skipping update."
  exit 0
fi

echo "Found ${CHANGELOG_FILE}..."

TEST_FOR_UNRELEASED=$(grep -F "## Unreleased" "${CHANGELOG_FILE}")
if [ -n "${TEST_FOR_UNRELEASED}" ]; then
  echo "${CHANGELOG_FILE} contains no Unreleased headline. Skipping update."
  exit 0
fi
TEST_FOR_VERSION=$(grep -F "## ${CHANGELOG_VERSION}" "./CHANGELOG.md")
if [ -n "$TEST_FOR_VERSION" ]; then
  echo "${CHANGELOG_FILE} already contains headline for this version (${CHANGELOG_VERSION}). Skipping update."
  exit 0
fi
echo "${CHANGELOG_FILE}: Replacing unreleased section with current version."
sed -i -e "s/## Unreleased/## Unreleased\\n\\nNo changes yet.\\n\\n## ${CHANGELOG_VERSION} - ${TODAY}/" "${CHANGELOG_FILE}"
