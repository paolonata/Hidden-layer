# Hidden Layer Launcher

Launcher Android sostitutivo, pensato per MIUI/HyperOS: home a pagine + dock
personalizzabile + cassetto delle app, esattamente come il
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
   **non compaiono nel cassetto e non escono nella ricerca**: si raggiungono
   con uno swipe verso l'alto a due dita dalla home, e nell'interfaccia non
   c'è nessun indizio che esistano.

## Come funziona

- **Home**: pagine scorrevoli con le icone che hai scelto di mettere lì,
  più il dock in basso (vedi sotto). Ogni pagina si riempie con quante righe di
  icone ci stanno davvero sullo schermo (calcolate sull'altezza reale del
  tuo display), non con un numero fisso.
- **Cassetto**: si apre **solo con lo swipe verso l'alto** — trascina dal
  dock verso l'alto, partendo sopra la zona del dock. Non c'è un pulsante
  dedicato: tutte le posizioni del dock restano libere per app vere.
  Il cassetto contiene tutte le app installate (tranne quelle nascoste), con
  ricerca, su uno sfondo sfocato che riprende il tuo sfondo reale. La griglia
  è a **5 icone per fila**, come il dock, sia nel cassetto normale sia in
  quello delle app nascoste.
- **Solo icone, niente nomi**: sotto le icone non c'è l'etichetta, né in home
  né nel cassetto — l'icona è già il nome. I nomi restano dove servono
  davvero a identificare un'app: nel menu che si apre tenendo premuto, nella
  richiesta di conferma della Concentrazione e negli elenchi con interruttore
  delle impostazioni (app nascoste e app da mettere in grigio), dove sono
  l'unico modo per capire su cosa stai agendo.
- **Tieni premuto** su un'icona per aprire un menu con le azioni disponibili
  a seconda di dove ti trovi: aggiungere/rimuovere dalla home o dal dock,
  spostare un'app alla pagina home precedente/successiva, vedere le info
  dell'app, disinstallarla, oppure **nasconderla**. Dal cassetto puoi mandare
  un'app **direttamente al dock**, senza doverla prima mettere in home.
- **Tocca uno slot vuoto del dock** (la crocetta) per scegliere subito
  un'app dall'elenco completo e metterla lì: non serve passare dalla home.
- **Sposta un'app tra le pagine home tenendola premuta**: tieni premuta
  un'icona in home e trascinala a sinistra o a destra (senza rilasciare) —
  superata una certa distanza va alla pagina precedente/successiva. Se la
  tieni premuta senza spostarla (o la sposti pochissimo) si apre invece il
  menu contestuale di cui sopra, come prima.
- **App nascoste**: si aprono con uno **swipe verso l'alto a due dita dalla
  home**, in un gesto solo e da qualunque punto (tranne la striscia in cima,
  lasciata alla tendina di sistema). Non c'è nessun'altra strada: niente voce
  di menu, niente pulsante, e soprattutto **niente da vedere** — l'ingresso
  non è un elemento piccolo da trovare, è un gesto che non lascia traccia
  sullo schermo. Ci si è arrivati per gradi: prima lo swipe a sinistra (un
  pager che si affacciava già durante il trascinamento, troppo facile da
  scovare), poi un doppio tap su due puntini in fondo al cassetto — ma un
  elemento disegnato, prima o poi, qualcuno lo tocca. Si esce come da ogni
  altra schermata — X, trascinamento verso il basso, tasto indietro — e si
  torna alla home. L'icona ingranaggio porta alle impostazioni, dove scegli
  quali app nascondere e se richiedere lo sblocco.
- **Il gesto nasconde la porta, non la chiude a chiave.** Se ti serve la
  certezza che nessun altro entri col telefono in mano, attiva **Richiedi
  sblocco** nelle impostazioni delle app nascoste: da lì in poi il gesto porta
  all'impronta (o al PIN), che sono meno di un secondo per te e un muro per
  chiunque altro. Lo sblocco vale **solo finché resti sulla schermata**:
  appena esci decade, quindi rifare il gesto dieci secondi dopo richiede di
  nuovo l'impronta.
- Tieni premuto su un punto vuoto della home per aggiungere un'app o
  cambiare sfondo (apre il selettore di sfondo di sistema).
- **Per chiudere** cassetto e app nascoste: trascina verso il basso da un
  punto qualsiasi (quando la griglia è già in cima), oppure dalla
  maniglietta in alto, oppure la X, oppure il tasto indietro di sistema.

## Concentrazione

Pensata per il momento in cui prendi il telefono senza motivo mentre lavori.

**Si avvia con un doppio tap su un punto vuoto della home**: compare un popup
con tre durate — **30 min, 1 ora, 2 ore** — e toccandone una la sessione
parte. Iniziare deve costare un gesto e una scelta, non quattro passaggi di
menu, perché è proprio quando ti servirebbe che hai meno voglia di cercarla;
tre opzioni e basta perché la scelta duri meno di un secondo. Il doppio tap
su un'icona resta due aperture dell'app, non avvia niente.

Se una sessione è già in corso — o se non hai ancora scelto le app da mettere
in grigio, perché allora non farebbe nulla — il doppio tap apre la schermata
Concentrazione invece di agire: un doppio tap involontario non può buttare
via il lavoro fatto.

**All'avvio, sopra il dock, appare per cinque secondi una pill col tempo
impostato**, poi sparisce da sola: è la conferma che la sessione è partita e
per quanto, non un elemento fisso della home. Finché è visibile la puoi
toccare per aprire Concentrazione (dove c'è "Termina ora"): fermarsi costa un
tocco in più che iniziare, di proposito. Che la sessione sia in corso lo
dicono comunque le icone grigie.

**Per una durata diversa dalle tre**: "Altra durata…" nel popup porta alla
schermata Concentrazione, che si apre anche tenendo premuto su un punto vuoto
della home. Lì scegli la durata con un tocco (15/30/45/60/120 min, più i
pulsanti ± per regolare di 5 in 5 fino a 3 ore), selezioni le app che ti
distraggono e avvii. Durante la sessione quelle app:

- **perdono il colore e sbiadiscono** ovunque — home, dock e cassetto. Il
  colore è metà del richiamo visivo di un'icona, ma da solo non basta: su
  un'icona già bianca, nera o grigia togliere la saturazione non cambia
  niente, e non si capiva quali app fossero bloccate. La trasparenza agisce
  sul rapporto con lo sfondo invece che sui colori dell'icona, quindi si vede
  sempre;
- **chiedono conferma** prima di aprirsi, con qualche secondo di attesa prima
  che il pulsante "Apri comunque" si attivi.

Il popup dice anche **da quanto stai resistendo** e quanto manca per battere
il tuo record — mai quanto sei lontano, sempre quanto manca: lì serve una
spinta, non un rimprovero.

Il punto non è vietare: raggiungere quell'app è un automatismo, l'attesa no,
e quando il pulsante si accende di solito la spinta è già passata. La
sessione ha una scadenza reale (un orario, non un conto alla rovescia in
memoria), quindi continua anche se MIUI chiude il launcher in background.

### Lo storico

In fondo alla schermata Concentrazione, sopra l'elenco delle app, c'è quello
che è successo davvero — di sempre, non dell'ultima sessione:

- il **record di resistenza**: il tratto più lungo passato sotto blocco senza
  aprire niente. Si azzera a ogni cedimento e non a ogni sessione, perché
  misura quanto riesci a resistere *di fila*, che è la cosa che si può
  battere. Una sessione finita senza cedimenti vale per intero — anche se
  scade mentre il launcher è chiuso;
- quante **sessioni** hai avviato, quante **aperture forzate** in tutto e la
  media per sessione;
- la **classifica delle app che apri comunque**, con quante volte e quando è
  successo l'ultima volta. È lì che si vede la tendenza: non cosa è successo
  martedì, ma quali app cedono sistematicamente.

Non c'è un registro delle singole sessioni di proposito, e c'è un "Azzera
statistiche" per ripartire da zero.

**Le app nascoste non entrano mai nello storico.** Il loro nome finirebbe
nella classifica dentro le impostazioni della Concentrazione, che non sono
protette dallo sblocco: sarebbe una falla nel senso stesso dell'area
riservata.

**Limite dichiarato**: un launcher non può impedire l'apertura di un'app. Le
app restano raggiungibili da notifiche, schermate recenti e ricerca di
sistema. Per un blocco vero serve la Modalità concentrazione di MIUI, che
agisce a livello di sistema.

## Riservatezza: cosa fa e cosa non può fare

**Cosa fa.** Le app nascoste spariscono da home, cassetto e ricerca del
launcher, e nulla nell'interfaccia rivela che esistono. L'elenco è cifrato a
riposo (AES-256-GCM, chiave nel Keystore hardware), quindi non è leggibile
da altre app senza root. In più:

- **Sblocco facoltativo** (interruttore nelle impostazioni): impronta con
  PIN di riserva. Attivarlo obbliga a impostare un PIN, perché la biometria
  può smettere di funzionare e non devi restare fuori dalle tue app.
  Consigliato proprio perché l'accesso è uno swipe: con lo sblocco attivo
  una scorsa accidentale finisce sulla richiesta del PIN, mai sulle app.
- **Niente anteprime né screenshot** mentre sei nell'area riservata
  (`FLAG_SECURE`): non compare nella schermata delle app recenti.
- **Richiusura automatica**: appena esci dal launcher — apri un'app, spegni
  lo schermo, cambi task — l'area si richiude. Riaprendo riparti dalla home,
  mai da dove eri rimasto. Ruotare lo schermo non conta come uscita, quindi
  non ti butta fuori mentre stai guardando.
- Se l'archivio cifrato non è leggibile (può succedere se la chiave del
  Keystore viene invalidata da un cambio di blocco schermo), il launcher parte
  lo stesso mostrando nessuna app nascosta, invece di non partire affatto.

**Cosa NON può fare.** Un launcher nasconde le app solo dentro di sé. Senza
root non è possibile toglierle da: Impostazioni > App, la ricerca globale di
MIUI, il Play Store, le notifiche, le statistiche di utilizzo e batteria, e
le schermate recenti dell'app stessa una volta aperta. Chi ispeziona
davvero il telefono le trova.

Detto in breve: protegge da chi dà un'occhiata al tuo telefono, non da chi
lo esamina. Se serve di più, su MIUI l'unica strada seria è il **Secondo
Spazio** di sistema.

## Dock: tre file da 5, le due superiori a scomparsa

Il dock è composto da **tre righe da 5 posizioni** (15 in totale). La riga in
basso è sempre visibile; le due sopra restano ripiegate e si aprono insieme
con uno **swipe verso l'alto sulla zona del dock**. Il gesto non va in
conflitto con quello che apre il cassetto: sopra il dock lo swipe verso
l'alto apre il cassetto, sul dock apre le file nascoste. A **due dita**,
ovunque, lo stesso movimento porta invece alle app nascoste.

Per **richiuderle**: swipe verso il basso sul dock, oppure un tocco o uno
swipe verso il basso in un punto qualsiasi dello schermo fuori dal dock,
oppure il tasto indietro. Con
le file aperte, un gesto fuori dal dock serve solo a richiuderle — non apre
l'app che hai toccato né cambia pagina, come da qualunque pannello aperto:
prima lo chiudi, poi la home torna a comportarsi normalmente. L'eccezione è
il **tenere premuto**: quello resta un'intenzione precisa, quindi apre il
menu contestuale o inizia un trascinamento, e le file restano aperte — è così
che ci si porta sopra un'icona della home.

Righe e colonne sono due costanti (`DOCK_ROWS` e `DOCK_COLUMNS` in
`HomeLayoutRepository`): il layout e il calcolo di dove atterra un'app
trascinata sono derivati da quelle, quindi passare a 6 per fila o a un
numero diverso di file è una modifica di una riga.

Un'app sta **o nel dock o nella home, mai in entrambi**: il dock è visibile
da tutte le pagine, quindi lasciarne una copia anche sulla griglia sarebbe
solo un doppione che occupa spazio. Mettendo un'app nel dock sparisce dalla
home, e viceversa. I layout salvati prima di questa regola vengono ripuliti
in automatico al primo avvio.

Puoi trascinare un'icona dalla home direttamente sul dock: atterra nello
slot su cui la rilasci, file superiori comprese. Anche le icone del dock si
trascinano, per spostarle tra slot e file o riportarle sulla griglia. Se
provi ad "Aggiungere al dock" con tutte e 15 le posizioni occupate, te lo
dice esplicitamente. Per liberare un posto: tieni premuto su un'icona del
dock → "Rimuovi dal dock" o "Sostituisci".

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
