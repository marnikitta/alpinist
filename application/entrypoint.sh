#!/usr/bin/env bash


main() {
  local params=(alpinist-application-1.0-SNAPSHOT.jar)

  if [[ -n $REMOTE && -n $REMOTE_KEY ]]; then
    mkdir -p ~/.ssh
    ssh-keyscan github.com >> ~/.ssh/known_hosts
    eval "$(ssh-agent -s)" && ssh-add "$REMOTE_KEY"
    params+=(--remote "$REMOTE")
  fi

  if [[ -n $BOT_NAME && -n $BOT_TOKEN && -n $BOT_OWNER ]]; then
    params+=(--username "$BOT_NAME")
    params+=(--token "$BOT_TOKEN")
    params+=(--owner "$BOT_OWNER")
  fi

  if [[ -n $LOCAL_DIR ]]; then
    params+=(--dir "$LOCAL_DIR")
  fi

  java -jar "${params[@]}"
}
main
