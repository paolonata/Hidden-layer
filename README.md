# Hidden Layer Launcher

Launcher Android sostitutivo, pensato per MIUI/HyperOS: home a pagine + dock
personalizzabile + cassetto delle app, esattamente come il layout "Home +
cassetto applicazioni" che MIUI offre di serie — con in più la possibilità di
**nascondere singole app**, che spariscono sia dalla home sia dal cassetto e
tornano visibili solo dopo aver sbloccato una sezione protetta da PIN (o
impronta, se disponibile). Nessun root richiesto.

## Come funziona

- **Home**: pagine scorrevoli con le icone che hai scelto di mettere lì, più
  un dock in basso a 4 posizioni personalizzabili + il pulsante "tutte le
  app".
- **Cassetto**: si apre trascinando verso l'alto la maniglietta sopra il dock
  oppure toccando l'icona "tutte le app" nel dock. Contiene tutte le app
  installate (tranne quelle nascoste), con ricerca.
- **Tieni premuto** su un'icona (home, dock o cassetto) per: aprire,
  aggiungere/rimuovere dalla home o dal dock, vedere le info dell'app,
  disinstallarla, oppure **nasconderla**.
- **App nascoste**: raggiungibili dal menu del cassetto ("⋮ → App
  nascoste") o tenendo premuto su un punto vuoto della home. La prima volta
  ti viene chiesto di impostare un PIN; le volte successive puoi sbloccare
  con impronta digitale (se il dispositivo la supporta) o con il PIN.
  Nascondere un'app è immediato e non richiede PIN — il PIN protegge solo
  la *visualizzazione/gestione* delle app nascoste.
- Tieni premuto su un punto vuoto della home per aggiungere un'app, cambiare
  sfondo (apre il selettore di sfondo di sistema) o accedere alle app
  nascoste.

## Sicurezza

- Il PIN non viene mai salvato in chiaro: viene applicato SHA-256 con salt
  casuale e il risultato è custodito in `EncryptedSharedPreferences`
  (AES-256-GCM, chiave nel Keystore hardware del dispositivo).
- L'elenco dei pacchetti nascosti è salvato con lo stesso meccanismo
  (`EncryptedSharedPreferences`), quindi illeggibile da altre app senza root.
- Tutto resta sul dispositivo: nessuna rete, nessuna sincronizzazione.

## Build

Serve Android Studio (Koala o successivo) oppure Gradle da riga di comando
con un Android SDK installato (`compileSdk 34`, `minSdk 26`).

```bash
./gradlew assembleDebug
```

L'APK generato è in `app/build/outputs/apk/debug/app-debug.apk`.

> Nota: questo progetto è stato scritto in un ambiente sandbox senza Android
> SDK né emulatore, quindi il codice **non è stato compilato né testato su
> un dispositivo reale** in questa sessione. È stato scritto e rivisto con
> cura seguendo le API stabili di Jetpack Compose/AndroidX, ma prima di
> fidartene vale la pena aprirlo in Android Studio, risolvere eventuali
> errori di compilazione residui e provarlo su un device o un emulatore.

## Installazione su MIUI/HyperOS

1. Abilita "Installa app sconosciute" per l'app che userai per installare
   l'APK (Impostazioni > App > Autorizzazioni speciali > Installa app
   sconosciute), poi installa l'APK.
2. Vai in **Impostazioni > App > App predefinite > Home app** (il nome
   esatto varia tra MIUI/HyperOS) e seleziona **Hidden Layer** come
   launcher predefinito.
3. Consigliato: in **Sicurezza > Autorizzazioni > Avvio automatico**
   abilita l'avvio automatico per l'app, e disattiva l'ottimizzazione
   batteria per essa (Impostazioni > App > Hidden Layer > Risparmio
   energetico > Nessuna restrizione). MIUI è aggressivo nel terminare i
   processi in background e un launcher che viene ucciso spesso perde
   reattività.
4. Alla prima apertura la home sarà popolata automaticamente con tutte le
   app visibili (le prime 4 nel dock, il resto sulle pagine home) — **non è
   possibile importare automaticamente la disposizione che avevi sul
   launcher MIUI precedente**: nessun launcher di terze parti può leggere il
   layout di un altro launcher senza root. Sistema le icone come preferisci,
   una volta sola.

## Limiti noti / possibili sviluppi futuri

- **Nessun supporto widget** in questa prima versione: richiede un
  sottosistema a parte (`AppWidgetHost`) che non è stato incluso per tenere
  lo scope gestibile e verificabile. Può essere aggiunto in un secondo
  momento.
- **Nessun riordino via drag & drop** sulle icone della home/dock: si
  rimuove e si riaggiunge un'app per spostarla (va in coda).
- Le interazioni gestuali (swipe-up per il cassetto, long-press) sono state
  scritte secondo i pattern standard di Jetpack Compose ma **non verificate
  su un device reale** in questa sessione.
