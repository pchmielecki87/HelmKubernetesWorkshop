# Build
docker build -t <new_docker_image_name> <path_to_Dockerfile>
docker build -t alpine-with-vim .

# List images
docker images

# Run
docker container run --name <give_a_name_of_container> -ti <image_name>
docker container run -ti alpine-with-vim /bin/sh
docker run -d busybox:1.24 sleep 100
docker run -d postgres sleep 100

# Connect to container
docker ps -a
<guid>
docker exec -it mynginxplus sh

# Exit
Exit or Ctrl+D

# Tag
docker tag <guid> <image_name>:<version-branch>

# Inspect JSON
docker inspect <container_id>

# Stop
docker stop <container>

# Rename
docker rename <current_container_name> <new_name>

# Remove
docker rm <container ID | container name>
docker run --rm busybox:1.24