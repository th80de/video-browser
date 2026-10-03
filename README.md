# Video Browser

Private Android-14+-App für YouTube-Suche, neue Videos abonnierter Kanäle, Favoriten und Suchverlauf. Die App spielt keine Inhalte selbst ab: Videos und Kanäle werden in der offiziellen YouTube-App geöffnet.

## Einrichten

1. In der Google Cloud Console ein Projekt anlegen und die **YouTube Data API v3** aktivieren.
2. Einen API-Key anlegen, auf Android-Apps (Paket `de.th80de.videobrowser` plus SHA-1 deines Signatur-Zertifikats) beschränken und als `YOUTUBE_API_KEY` in `secrets.properties` eintragen. `secrets.properties` ist ignoriert und wird nie committed. Die App übermittelt den Schlüssel zusammen mit Paketname und Zertifikats-Fingerabdruck in den von Google geforderten HTTP-Headern (der Header-Fingerabdruck ist Hex ohne Doppelpunkte).
3. Den OAuth-Zustimmungsbildschirm konfigurieren und einen Android OAuth Client für dasselbe Paket und SHA-1 anlegen. Der Client braucht den Scope `https://www.googleapis.com/auth/youtube.readonly`.
4. Projekt in Android Studio öffnen und auf einem Android-14+-Gerät ausführen. Verwende das mit Android Studio ausgelieferte JDK 17 oder 21; das lokale Oracle JDK 25 ist für dieses Tooling nicht geeignet.

Für den persönlichen Feed meldest du dich optional mit Google an. Suche, Favoriten und Verlauf funktionieren ohne Anmeldung; ohne API-Key ist die Suche absichtlich deaktiviert.

Premium Lite, Werbung, Downloads und Hintergrundwiedergabe werden nicht von dieser App verändert. Die Wiedergabe erfolgt in der offiziellen YouTube-App.
