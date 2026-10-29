# Rabaty — aplikacja Android (wersja startowa)

Natywny projekt Android w Kotlinie i Jetpack Compose. Pobiera listę grup z endpointu:

`https://vidad.webd.pl/rabaty/wp-json/rabaty/v1/groups`

## Funkcje tej wersji
- natywny interfejs Android (bez WebView),
- pobieranie grup przez HTTPS,
- cena grupowa, liczba uczestników i pasek postępu,
- stan ładowania, błąd sieci i odświeżanie.

## Ograniczenia
- To projekt źródłowy, nie gotowy APK.
- Nie ma jeszcze logowania, dołączania do grup, koszyka ani płatności.
- Nie przechowuje haseł ani kluczy API.
- Dane API są wyświetlane w formie podstawowej; trzeba zweryfikować zasady biznesowe i statusy na prawdziwych grupach.

## Jak uruchomić
1. Zainstaluj Android Studio (aktualną stabilną wersję) i Android SDK Platform 35.
2. Otwórz w Android Studio folder `rabaty-android`.
3. Poczekaj na synchronizację Gradle i zaakceptuj instalację wymaganych komponentów SDK.
4. Uruchom emulator z Androidem 8.0+ albo podłącz telefon z włączonym debugowaniem USB.
5. Kliknij **Run**.
6. Aby zbudować APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**. Plik debug APK zwykle pojawi się w `app/build/outputs/apk/debug/app-debug.apk`.

Wymagane środowisko kompilacji: JDK 17 oraz połączenie z internetem podczas pierwszej synchronizacji Gradle.
