LOG_CONTAINER?=java_spring_app
LOG_TAIL_N?=20

SHELL=/bin/bash

docker_build:
	docker compose -f docker/docker-compose.yml --project-directory ./docker build --no-cache

docker_deploy:
	docker compose -f docker/docker-compose.yml --project-directory ./docker up -d

docker_stop:
	docker compose -f docker/docker-compose.yml --project-directory ./docker down

docker_log:
	docker logs -t -f --tail $(LOG_TAIL_N) $(LOG_CONTAINER)

docker_remove_all_volume:
	docker volume rm $(docker volume ls -q)

docker_run:
	docker-compose up --build -V --force-recreate

docker_run_detach:
	docker-compose up --build -V --force-recreate -d && docker-compose logs -f