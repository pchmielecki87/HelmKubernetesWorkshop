# C# .NET Core 9

## Prepare app

```csharp
dotnet new web -n DockerContainerizedApp
cd DockerContainerizedApp
dotnet publish -c Release -o out
```

## Prepare and run Dockerfile

docker build -t dotnet9app .
docker run -d -p 8080:80 --name dotnet9app dotnet9app
http://localhost:8080

## (optional) Use Docker Compose

`docker-compose up -d`

## Stop and remove container

`docker stop <container_name>`
`docker rm <container_name>`
