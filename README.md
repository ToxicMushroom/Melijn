# Melijn
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/60761596055e49e88d9b8db1ffa65fdf)](https://www.codacy.com/manual/ToxicMushroom/Melijn?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ToxicMushroom/Melijn&amp;utm_campaign=Badge_Grade)
[![Discord Bots](https://discordbots.org/api/widget/servers/368362411591204865.svg?noavatar=true)](https://discordbots.org/bot/368362411591204865)
[![Discord Bots](https://discordbots.org/api/widget/status/368362411591204865.svg?noavatar=true)](https://discordbots.org/bot/368362411591204865)
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2FToxicMushroom%2FMelijn.svg?type=shield)](https://app.fossa.io/projects/git%2Bgithub.com%2FToxicMushroom%2FMelijn?ref=badge_shield)


[![Bots for Discord](https://botsfordiscord.com/api/bot/368362411591204865/widget)](https://botsfordiscord.com/bots/368362411591204865)
[![Discord Boats](https://discord.boats/api/widget/368362411591204865)](https://discord.boats/api/widget/368362411591204865)

[![forthebadge](https://forthebadge.com/images/badges/works-on-my-machine.svg)](https://forthebadge.com)


If you want to use code from Melijn you have to credit me and all our contributors. 
Contact me on discord for more information ToxicMushroom#0001

(I do not support changing the source code nor helping to build the source code.)

## How to self-host using [docker](https://docs.docker.com/get-docker/)
1. Copy `example-docker-compose.yml` to `docker-compose.yml`
2. Copy `example-postgres.env` to `postgres.env`
3. Copy `example.env` to `.env`
4. Uncomment one of the volumes in the melijn-postgres service for persistant storage
5. Make sure all the passwords, usernames and database names match between your `.env` file and the postgres and redis services
6. Fill in all the fields you can in `.env`, bot token, id, name, etc... (If you don't have tokens for spotify ect then don't expect commands related to spotify to work)
> These 2 steps are optional (see notes):<br>
>7. You can build your own image from the source-code using `sudo docker build -t username/melijn:version .`<br>
>8. Replace `toxicmushroom/Melijn:version` in `docker-compose.yml` with your `username/melijn:version`
9. Start docker compose with `sudo docker-compose up -d` (Make sure you're in the same folder as docker-compose.yml)

NOTES: 
- username/melijn:version can be replaced by eg. `ShadowGamer/melijn:v1`
- if you want to use your image you built on another machine then read the docker docs about `docker login` and `docker push` (you can publish images for free on https://hub.docker.com/)
- You can also skip steps 7 and 8 and checkout what the latest tag is on https://hub.docker.com/r/toxicmushroom/melijn (currently this is `toxicmushroom/melijn:14e88d3ab1afe5bba3d7442488a768f13e3e0077`). Make sure to replace the example tag in the `docker-compose.yml` with the latest tag.

Note. If required you can copy this environment folder anywhere your like, rename it, ect.
Be aware that this contains the login information to the local postgres storage which might hold user data collected from your discord server by melijn.
You should never share your environment with others.

## License
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2FToxicMushroom%2FMelijn.svg?type=large)](https://app.fossa.io/projects/git%2Bgithub.com%2FToxicMushroom%2FMelijn?ref=badge_large)
