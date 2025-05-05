# ASP.NET Core 8.0

## Create ASP.NET app
dotnet new blazorwasm -n MyBlazorApp
cd MyBlazorApp

## Build Docker image and run it
docker build -t myblazorapp .
docker run -d -p 8080:80 --name myblazorcontainer myblazorapp

## Stop and remove container
docker stop <container_name>
docker rm <container_name>
