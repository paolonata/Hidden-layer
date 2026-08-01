# Hidden Layer Launcher

Launcher Android sostitutivo, pensato per MIUI/HyperOS: home a pagine + dock
personalizzabile (5 posizioni) + cassetto delle app, esattamente come il
layout "Home + cassetto applicazioni" che MIUI offre di serie — con in più
un'area riservata per le app che non vuoi far vedere. Nessun root richiesto.

## Tre livelli, non due

Sono due esigenze diverse e vanno tenute separate, altrimenti si finisce per
usare lo strumento della riservatezza per fare le pulizie:

1. **Home** — solo i tuoi preferiti. Togliere un'app dalla home *non* la
   nasconde: sparisce dalla home e basta, resta nel cassetto.
2. **Cassetto** — l'indice completo delle app installate, comprese quelle in
   home. È dove cerchi ciò che usi di rado. Nessun blocco, nessuna cerimonia.
3. **App nascoste** — le poche app davvero private. Non compaiono in home,
   **non compaiono nel cassetto e non escono nella ricerca**: esistono solo
   dentro la loro area.

## Come funziona

- **Home**: pagine scorrevoli con le icone che hai scelto di mettere lì,
  più un dock in basso a 5 posizioni, tutte assegnabili liberamente (icona
  in basso a destra compresa). Ogni pagina si riempie con quante righe di
  icone ci stanno davvero sullo schermo (calcolate sull'altezza reale del
  tuo display), non con un numero fisso.
- **Cassetto**: si apre **solo con lo swipe verso l'alto** — trascina dal
  dock verso l'alto (o dalla maniglietta sopra di esso). Non c'è un pulsante
  dedicato: tutte e 5 le posizioni del dock restano libere per app vere.
  Il cassetto contiene tutte le app installate (tranne quelle nascoste), con
  ricerca, su uno sfondo sfocato che riprende il tuo sfondo reale.
- **Tieni premuto** su un'icona per aprire un menu con le azioni disponibili
  a seconda di dove ti trovi: aggiungere/rimuovere dalla home o dal dock,
  spostare un'app alla pagina home precedente/successiva, vedere le info
  dell'app, disinstallarla, oppure **nasconderla**.
- **Sposta un'app tra le pagine home tenendola premuta**: tieni premuta
  un'icona in home e trascinala a sinistra o a destra (senza rilasciare) —
  superata una certa distanza va alla pagina precedente/successiva. Se la
  tieni premuta senza spostarla (o la sposti pochissimo) si apre invece il
  menu contestuale di cui sopra, come prima.
- **App nascoste**: raggiungibili dal menu del cassetto ("⋮ → App
  nascoste") o tenendo premuto su un punto vuoto della home. Si apre un
  cassetto a parte, con sfondo più scuro, che contiene solo le app nascoste:
  ricerca e tocco per aprirle. L'icona ingranaggio lì dentro porta alle
  impostazioni, dove scegli quali app nascondere e se richiedere lo sblocco.
- Tieni premuto su un punto vuoto della home per aggiungere un'app, cambiare
  sfondo (apre il selettore di sfondo di sistema) o accedere alle app
  nascoste.
- **Per chiudere** cassetto e app nascoste: trascina verso il basso da un
  punto qualsiasi (quando la griglia è già in cima), oppure dalla
  maniglietta in alto, oppure la X, oppure il tasto indietro di sistema.

## Riservatezza: cosa fa e cosa non può fare

**Cosa fa.** Le app nascoste spariscono da home, cassetto e ricerca del
launcher. L'elenco è cifrato a riposo (AES-256-GCM, chiave nel Keystore
hardware), quindi non è leggibile da altre app senza root. In più:

- **Sblocco facoltativo** (interruttore nelle impostazioni): impronta con
  PIN di riserva. Attivarlo obbliga a impostare un PIN, perché la biometria
  può smettere di funzionare e non devi restare fuori dalle tue app.
- **Niente anteprime né screenshot** mentre sei nell'area riservata
  (`FLAG_SECURE`): non compare nella schermata delle app recenti.
- **Richiusura automatica**: appena esci dal launcher — apri un'app, spegni
  lo schermo, cambi task — l'area si richiude. Riaprendo riparti dalla home,
  mai da dove eri rimasto.

**Cosa NON può fare.** Un launcher nasconde le app solo dentro di sé. Senza
root non è possibile toglierle da: Impostazioni > App, la ricerca globale di
MIUI, il Play Store, le notifiche, le statistiche di utilizzo e batteria, e
le schermate recenti dell'app stessa una volta aperta. Chi ispeziona
davvero il telefono le trova.

Detto in breve: protegge da chi dà un'occhiata al tuo telefono, non da chi
lo esamina. Se serve di più, su MIUI l'unica strada seria è il **Secondo
Spazio** di sistema.

## Dock: limite di 5 app

Il dock ha esattamente 5 posizioni (tante quante le icone mostrate in
basso) — è un limite fisso, non un bug. Al primo avvio viene riempito
automaticamente con le prime 5 app in ordine alfabetico insieme al resto
della home; se provi ad "Aggiungere al dock" un'altra app mentre è pieno,
ora te lo dice esplicitamente invece di non fare nulla (prima versione:
falliva silenziosamente). Per liberare un posto: tieni premuto su un'icona
del dock → "Rimuovi dal dock" o "Sostituisci".

## Build

Serve Android Studio (Koala o successivo) oppure Gradle da riga di comando
con un Android SDK installato (`compileSdk 34`, `minSdk 26`).

```bash
./gradlew assembleDebug
```

L'APK generato è in `app/build/outputs/apk/debug/app-debug.apk`. In
alternativa, il repository include un workflow GitHub Actions
(`.github/workflows/build.yml`) che compila l'APK a ogni push e lo carica
come artifact scaricabile dalla scheda Actions.

### Firma: perché gli aggiornamenti si installano sopra

Gli APK sono firmati con la chiave in `keystore/hiddenlayer.p12`, versionata
nel repository apposta. Senza di essa ogni build su GitHub userebbe un debug
keystore generato al momento sul runner (macchina effimera, chiave diversa
ogni volta) e Android rifiuterebbe l'installazione sopra la versione già
presente — costringendo a disinstallare e quindi a perdere app nascoste e
disposizione delle icone a ogni prova.

Il `versionCode` segue il numero della build di GitHub Actions, così ogni
APK conta come aggiornamento e non come downgrade (il `versionName` mostra
lo stesso numero, utile per capire quale build è installata).

Nota: è una chiave usa e getta per installazioni locali, non un segreto di
distribuzione — chiunque abbia il repository può firmare un APK con lo
stesso identificativo. Per un'app personale sideloaded è un compromesso
ragionevole; se preferisci non averla nel repository, si può spostare in un
secret di GitHub Actions (base64) e ricrearla nel workflow al volo.

> Nota: questo progetto è stato scritto in un ambiente sandbox senza Android
> SDK né emulatore, quindi buona parte del codice non è stata testata su un
> dispositivo reale. È stato scritto e rivisto con cura seguendo le API
> stabili di Jetpack Compose/AndroidX, ma se qualcosa si comporta in modo
> imprevisto (gesture, animazioni, blur) è utile saperlo per segnalarlo.

## Installazione su MIUI/HyperOS

1. Abilita "Installa app sconosciute" per l'app che userai per installare
   l'APK (Impostazioni > App > Autorizzazioni speciali > Installa app
   sconosciute), poi installa l'APK.
   > Se hai già installato una build precedente alla firma stabile
   > (vedi sopra), quella volta lì serve **un'ultima disinstallazione**:
   > la vecchia copia è firmata con una chiave casuale e non è
   > aggiornabile. Da quella in poi gli APK si installano sopra e le tue
   > impostazioni restano.
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
   app visibili (le prime 5 nel dock, il resto sulle pagine home) — **non è
   possibile importare automaticamente la disposizione che avevi sul
   launcher MIUI precedente**: nessun launcher di terze parti può leggere il
   layout di un altro launcher senza root. Sistema le icone come preferisci,
   una volta sola.

Per tornare al launcher MIUI o disinstallare: vedi le istruzioni date in
chat (Impostazioni > App > App predefinite > Home app, oppure Disinstalla
dal menu contestuale di un'icona o da Gestisci app).

## Limiti noti / possibili sviluppi futuri

- **Nessun supporto widget** in questa prima versione: richiede un
  sottosistema a parte (`AppWidgetHost`) che non è stato incluso per tenere
  lo scope gestibile e verificabile. Può essere aggiunto in un secondo
  momento.
- **Nessun riordino via drag & drop all'interno della stessa pagina**: si
  può spostare un'app da una pagina all'altra tenendola premuta (o dal menu
  contestuale), ma non ancora cambiarne la posizione tra le icone della
  stessa pagina.
- Lo sfondo sfocato del cassetto/app nascoste è calcolato una sola volta
  (blur software su una copia piccola dello sfondo, non un effetto live),
  quindi funziona su tutte le versioni Android supportate (26+), non solo
  da Android 12 in su.
- Le interazioni gestuali (swipe-up, swipe-down, drag tra pagine) e le
  animazioni sono state scritte secondo i pattern standard di Jetpack
  Compose ma non tutte verificate su un device reale in questa sessione —
  se qualcosa si comporta in modo imprevisto, segnalalo pure.
