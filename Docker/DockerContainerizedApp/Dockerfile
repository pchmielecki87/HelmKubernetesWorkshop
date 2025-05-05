FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
WORKDIR /app
COPY DockerContainerizedApp/*.csproj ./
RUN dotnet restore
COPY DockerContainerizedApp/. ./
RUN dotnet publish -c Release -o out

FROM mcr.microsoft.com/dotnet/aspnet:9.0 AS runtime
WORKDIR /app
COPY --from=build /app/out ./
ENTRYPOINT ["dotnet", "DockerContainerizedApp.dll"]
