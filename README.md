# Katalog włóczki

Prosta aplikacja na Androida (7.0+, API 24) do katalogowania zapasów włóczki.
Kotlin, zwykłe widoki Androida i SQLite, bez dodatkowych bibliotek.

## Funkcje

- Pola: **grubość, rodzaj, kolor, ilość, miejsce składowania, zdjęcie** (z aparatu lub galerii).
  Zdjęcie jest zmniejszane do 1600 px, obracane wg EXIF i zapisywane w pamięci aplikacji.
- Lista wszystkich włóczek z miniaturą, licznikiem pozycji i motków.
- Przyciski **+ / −** przy każdej pozycji. Zejście do 0 pyta o potwierdzenie i usuwa pozycję.
- Dotknięcie pozycji otwiera edycję wszystkich pól. Przycisk **+ Dodaj** dodaje nową.
- Wyszukiwanie na żywo (bez rozróżniania wielkości liter) w wybranym polu
  (Wszystkie pola / Grubość / Rodzaj / Kolor / Ilość / Miejsce) i przycisk **Pokaż wszystkie**.
- Menu ⋮: **Eksportuj do pliku** – jeden plik `katalog-wloczki.zip` (JSON + zdjęcia),
  **Importuj z pliku** – z wyborem *Zastąp* lub *Dodaj*. Służy np. do przeniesienia danych na nowy telefon.
- Pełny ekran (edge-to-edge): przezroczyste paski systemowe, a elementy nie chowają się
  pod paskiem nawigacji (gesty i 3 przyciski) ani pod klawiaturą.

## Budowanie

Wymagane: JDK 17 i Android SDK (platforma 34).

```bash
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`.

### Podpisywanie

Klucz podpisu **nie jest** w repozytorium. Bez niego wydanie zostanie podpisane kluczem debug
(projekt zawsze się zbuduje, ale takiej wersji nie da się zainstalować jako aktualizacji
oficjalnej wersji).

Aby podpisać właściwym kluczem, podaj plik `keystore.properties`:

```properties
storeFile=/ścieżka/do/release.jks
storePassword=...
keyAlias=katalog
keyPassword=...
```

na jeden ze sposobów:

- `./gradlew assembleRelease -PkeystoreProps=/ścieżka/keystore.properties`
- zmienna środowiskowa `KATALOG_KEYSTORE_PROPERTIES=/ścieżka/keystore.properties`
- plik `keystore.properties` w katalogu głównym projektu (jest w `.gitignore`)
- albo zmienne `KATALOG_STORE_FILE`, `KATALOG_STORE_PASSWORD`, `KATALOG_KEY_ALIAS`, `KATALOG_KEY_PASSWORD`.

Przechowuj klucz i hasła w bezpiecznym miejscu – bez nich nie da się wydać aktualizacji.
