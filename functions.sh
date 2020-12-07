#!/usr/bin/env bash

readonly WIKI_BASE=${WIKI_BASE:?Need to set WIKI_BASE non-empty}

filefy() {
  local string=$1
  echo "$string" \
  | sed -e 's/[^[:alnum:] ]//g' \
  | sed -e 's/[[:space:]]\+/_/g' \
  | tr '[:upper:]' '[:lower:]'
}

create_ghost_file() {
  if [[ $# -ne 2 ]]; then
    echo "Usage: create_ghost_file <file_name> <header>"
    return 1
  fi
  local dst=$1
  local header=$2
  local header_lines=$(echo "$header" | wc -c)
  if [[ ! -e $dst ]]; then
    touch "$dst"
    echo "$header" > "$dst"
  fi

  vim "$dst"

  # remove file if there are no additional lines
  if [[ $(wc -c < "$dst") -eq $header_lines ]]; then
    rm "$dst"
  fi
}

lb() {
  local file_date=$(date +'%Y-W%V')
  cd ${WIKI_BASE}
  create_ghost_file "${WIKI_BASE}/logbook/${file_date}.md" "# ${file_date}\\n\\n[[logbook]]\\n"
  cd -
}

lbt() {
  local file_date=$(date +'%F')
  cd ${WIKI_BASE}
  create_ghost_file "${WIKI_BASE}/logbook/${file_date}.md" "# ${file_date}\\n\\n[[logbook]]\\n"
  cd -
}

li() {
  if [[ $# -ne 2 ]]; then
    echo "Usage: li <title> <url>"
    return 1
  fi
  local title=$1
  local url=$2
  local file_name=$(filefy "$title")

  create_ghost_file "${WIKI_BASE}/link/${file_name}.md" "# [${title}](${url})\\n"
}

note() {
  if [[ $# -ne 1 ]]; then
    echo "Usage: note <name>"
    return 1
  fi
  local title=$1
  local file_name=$(filefy "$title")

  create_ghost_file "${WIKI_BASE}/note/${file_name}.md" "# ${title}\\n"
}
