docker-compose up -d

# Wyjaśnienie YAML
- version: '3.8': Określa wersję składni Docker Compose.
- services: Definiuje poszczególne kontenery, które zostaną uruchomione.

- db: Definiuje kontener bazy danych MySQL.
- image: mysql:5.7: Używa oficjalnego obrazu MySQL w wersji 5.7 z Docker Hub. Możesz zmienić wersję, jeśli potrzebujesz innej.
- restart: always: Sprawia, że kontener MySQL będzie automatycznie restartowany w przypadku awarii.
- environment: Ustawia zmienne środowiskowe potrzebne do konfiguracji MySQL:
  - MYSQL_ROOT_PASSWORD: Hasło dla użytkownika root MySQL. Zmień example_root_password na bezpieczne hasło.
  - MYSQL_DATABASE: Nazwa bazy danych, która zostanie utworzona (tutaj wordpress).
  - MYSQL_USER: Nazwa użytkownika, który będzie miał dostęp do bazy danych WordPressa (tutaj wordpress).
  - MYSQL_PASSWORD: Hasło dla użytkownika WordPressa. Zmień example_wordpress_password na bezpieczne hasło.
- volumes: Montuje wolumeny, aby dane bazy danych były trwałe (nie były usuwane po zatrzymaniu kontenera).
- db_data:/var/lib/mysql: Mapuje nazwany wolumen db_data na ścieżkę wewnątrz kontenera, gdzie przechowywane są dane MySQL.
- ports: Mapuje porty między hostem a kontenerem.
- "3306:3306": Mapuje port 3306 na Twoim komputerze na port 3306 w kontenerze MySQL. Możesz usunąć to mapowanie w środowisku produkcyjnym, aby zabezpieczyć bazę danych przed dostępem z zewnątrz.

- wordpress: Definiuje kontener WordPressa.
- image: wordpress:latest: Używa najnowszej oficjalnej wersji obrazu WordPressa z Docker Hub.
- depends_on: Określa, że kontener wordpress powinien zostać uruchomiony dopiero po uruchomieniu kontenera db.
- restart: always: Sprawia, że kontener WordPressa będzie automatycznie restartowany w przypadku awarii.
- ports: Mapuje porty między hostem a kontenerem.
- "80:80": Mapuje port 80 na Twoim komputerze na port 80 w kontenerze WordPressa (standardowy port HTTP).
- environment: Ustawia zmienne środowiskowe potrzebne do połączenia WordPressa z bazą danych:
  - WORDPRESS_DB_HOST: Nazwa hosta bazy danych (tutaj nazwa serwisu db).
  - WORDPRESS_DB_USER: Nazwa użytkownika bazy danych WordPressa (tutaj wordpress).
  - WORDPRESS_DB_PASSWORD: Hasło użytkownika bazy danych WordPressa (tutaj example_wordpress_password).
  - WORDPRESS_DB_NAME: Nazwa bazy danych WordPressa (tutaj wordpress).
- volumes: Montuje wolumeny, aby pliki WordPressa (motywy, wtyczki, przesłane media) były trwałe.
- wordpress_data:/var/www/html: Mapuje nazwany wolumen wordpress_data na ścieżkę wewnątrz kontenera, gdzie znajdują się pliki WordPressa.
- volumes: Definiuje nazwane wolumeny, które zostały użyte w sekcji services. Docker zarządza tymi wolumenami, zapewniając trwałość danych.