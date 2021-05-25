1. Copy `example-docker-compose.yml` to `docker-compose.yml`
2. Copy `example-postgres.env` to `postgres.env`
3. Make sure the `docker-compose.yml` references the `postgres.env` and not `example-postgres.env`
4. Uncomment one of the volumes in the melijn-postgres service for persistant storage
5. Make sure all the passwords, usernames and database names match between your `.env` and this environment
6. Start docker compose with `sudo docker-compose up -d` (You have to be in the environment folder to run this)

Note. If required you can copy this environment folder anywhere your like, rename it, ect.
Be aware that this contains the login information to the local postgres storage which might hold user data collected from your discord server by melijn.
You should never share your environment with others.