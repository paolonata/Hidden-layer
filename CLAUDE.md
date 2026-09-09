# Hidden Layer Launcher — note per Claude

Launcher Android sostitutivo per MIUI/HyperOS, senza root. Kotlin + Jetpack
Compose. Vedi `README.md` per il comportamento dal punto di vista dell'utente;
questo file è la memoria di lavoro: cos'è la versione di riferimento, cosa è
già stato provato, e dove sono le trappole.

---

## 1. Versione di riferimento per il troubleshooting

> **Riferimento corrente: `1.0.36` — commit `cc2d905` — build GitHub Actions #36.**
>
> Ogni volta che si indaga un problema (lag, gesture che non rispondono,
> regressioni), **il confronto si fa con questa versione**, non con `HEAD`.
> Se una modifica peggiora qualcosa, si torna qui e si riapplica il minimo
> indispensabile, invece di stratificare correzioni sopra correzioni.
>
> **Questo riferimento si cambia solo se l'utente lo dice esplicitamente**
> ("usa la 1.0.40 come riferimento"). In quel caso: aggiorna questa sezione con
> nuova versione, commit e cosa contiene, e sposta la vecchia nello storico
> qui sotto.

Per ripartire dal riferimento:

```bash
git checkout cc2d905 -- app README.md
```

Cosa contiene la 1.0.36, in breve:

- home a pagine (4 colonne) + dock 3×5 con le due file superiori a scomparsa,
  che si richiudono con un gesto fuori dal dock;
- cassetto a 5 colonne, **senza etichette sotto le icone** (solo icone,
  ovunque tranne nelle liste con interruttore delle impostazioni, nel menu
  contestuale e nella conferma della Concentrazione);
- app nascoste raggiungibili **solo** col doppio tap sui due puntini in fondo
  al cassetto — niente più pager né swipe a sinistra; stile incognito, sblocco
  biometrico/PIN facoltativo;
- Concentrazione: doppio tap sulla home → popup 30 min / 1 ora / 2 ore → pill
  col countdown sopra il dock per 5 secondi, poi sparisce; storico di sempre
  delle aperture forzate e record di resistenza, mostrato anche nel popup di
  conferma;
- app in grigio riconoscibili anche quando l'icona è già in bianco e nero
  (scolorite **e** sbiadite, `MUTED_ALPHA`);
- popup con carta scura piena, leggibili anche sopra la griglia del cassetto;
- versione installata in fondo alle impostazioni delle app nascoste;
- tutte le correzioni dell'audit: le quattro falle di privacy, le due
  regressioni della chiusura del dock, l'archivio cifrato che degrada invece
  di far crashare il launcher, il layout che non viene più riscritto, e lo
  sfondo sfocato in cache.

### Storico dei riferimenti

| Versione | Commit | Perché era il riferimento |
|---|---|---|
| 1.0.20 | `c712d9a` | Ultima considerata veloce dall'utente. Le build 21–23 hanno perso reattività e le correzioni non l'hanno recuperata, quindi la 1.0.24 è stata ricostruita ripartendo da qui. |
| 1.0.24 | `1997f45` | 1.0.20 + solo la Concentrazione a doppio tap, senza le ottimizzazioni stratificate nelle build 21–23. |
| 1.0.33 | `cd61cda` | La 1.0.24 più lo storico della Concentrazione, le correzioni dell'audit e i popup leggibili. |
| 1.0.36 | `cc2d905` | **Attuale.** La 1.0.33 più le app in grigio riconoscibili, la carta dei popup opaca e la versione visibile nell'app. Confermata buona dall'utente. |

---

## 2. Build: si compila solo su GitHub Actions

**In questo ambiente non c'è l'Android SDK e il proxy blocca `dl.google.com`,
quindi `./gradlew` non funziona e non si può verificare la compilazione in
locale.** L'unico modo di sapere se il codice compila è il push.

Flusso normale dopo una modifica:

1. commit + `git push -u origin claude/miui-hidden-app-drawer-nwqv9u`
   (**sempre questo branch**, mai un altro senza permesso esplicito);
2. programma un check con `send_later` a ~4 minuti;
3. trova il run: `mcp__github__actions_list` con `method=list_workflow_runs`,
   `resource_id=build.yml`, `per_page=2`. **L'output supera il limite di token
   e finisce in un file**: parsalo con python leggendo
   `id / run_number / head_sha / status / conclusion / html_url`;
4. se è verde, dai all'utente il link al run — **l'APK lo scarica lui dal
   browser**, dalla sezione Artifacts. L'host degli artifact
   (`productionresultssa3.blob.core.windows.net`) è bloccato dal proxy, quindi
   qui non è scaricabile.

Dettagli: `versionCode` = numero di build Actions, `versionName` = `1.0.<build>`.
Firma con `keystore/hiddenlayer.p12` versionato apposta (password e alias
`hiddenlayer`) — senza, ogni build userebbe una chiave diversa e Android
rifiuterebbe l'aggiornamento sopra l'installazione esistente. È una chiave usa
e getta per sideload, non un segreto di distribuzione; il compromesso è stato
dichiarato all'utente. `compileSdk 34`, `minSdk 26`, JVM target 17.

---

## 3. Architettura

Un solo `MainActivity` (`FragmentActivity`, serve a BiometricPrompt) →
`LauncherApp` → `AnimatedContent` su `Screen`. Nessun nav graph.

```
LauncherViewModel
  ├─ uiState: StateFlow<LauncherUiState>   ← raccolto alla RADICE da LauncherApp
  └─ focusRemainingSeconds: StateFlow<Int> ← raccolto SOLO nelle foglie
```

`LauncherUiState` è una data class con proprietà **calcolate** (`homePages`,
`dockSlots`, `visibleApps`, `hiddenApps`…): rifanno il lavoro a ogni lettura.
Va bene finché lo stato cambia raramente, diventa un problema se qualcosa lo
aggiorna di continuo.

Repository su `SharedPreferences`, tranne le app nascoste e l'hash del PIN che
stanno in `EncryptedSharedPreferences` (AES-256-GCM, chiave nel Keystore).

**Visibilità dei package (Android 11+)**: l'app vede solo ciò che dichiara in
`<queries>` nel manifest. Ogni intent implicito verso un'altra app va dichiarato
lì, altrimenti `startActivity` solleva `ActivityNotFoundException` — e per un
launcher significa crash del processo home e riavvio istantaneo sulla home, che
da fuori sembra **"il menu non fa niente"** (era il bug di "Disinstalla"). Per
lo stesso motivo ogni `startActivity` di `AppRepository` passa da `start()`, che
cattura l'eccezione invece di far cadere il launcher. La disinstallazione vuole
anche il permesso `REQUEST_DELETE_PACKAGES`.

`HomeLayoutRepository`: gli item della home sono una **lista ordinata senza
buchi**; il dock è una **griglia a slot fissi** (`DOCK_ROWS × DOCK_COLUMNS`,
con `null` dove è vuoto) — serve a far atterrare un drop nella riga giusta.
Un'app sta **o nel dock o nella home, mai in entrambi**.

### Luminosità extra (`ScreenDimService`)

Un velo nero in una finestra `TYPE_APPLICATION_OVERLAY`, tenuta in piedi da un
servizio in foreground. Trappole già pagate, tutte da rispettare se si tocca
questo file o se ne scrive un altro simile:

- `startForeground` va chiamato **per primo in `onStartCommand`, anche sul
  ramo che spegne**: arrivando da `startForegroundService` il sistema pretende
  la promozione entro pochi secondi, altrimenti uccide il processo con
  `ForegroundServiceDidNotStartInTimeException`. Per questo `stop()` dal
  launcher usa `stopService`, che quel vincolo non ce l'ha.
- Da `targetSdk` 34 il servizio deve dichiarare `foregroundServiceType`: qui
  `specialUse` con la `<property>` che lo motiva, perché nessuno dei tipi
  previsti descrive "tenere in piedi un overlay".
- **`FLAG_LAYOUT_NO_LIMITS` da solo non copre il notch**: serve anche
  `layoutInDisplayCutoutMode`, altrimenti resta una striscia non attenuata in
  cima. Stesso bug già visto e corretto su `HomeLockOverlay`.
- **`MATCH_PARENT` su questa finestra non è "tutto lo schermo".** A differenza
  di `HomeLockOverlay` — un `Dialog`, legato al token della finestra del
  launcher — questo è un `TYPE_APPLICATION_OVERLAY` a sé, e su MIUI/HyperOS
  quel tipo di finestra viene ridimensionato per stare sotto le barre di
  sistema anche con `FLAG_LAYOUT_NO_LIMITS`: restavano due strisce a piena
  luminosità in cima e in fondo, segnalato dall'utente con screenshot. La
  correzione chiede i pixel **fisici** reali (`Display.getRealSize`, deprecato
  ma l'unico modo fino a `minSdk` 26) e li passa come larghezza/altezza
  esplicite invece di `MATCH_PARENT`. Se si tocca di nuovo questo file, non
  tornare a `MATCH_PARENT` "perché è più pulito": è quello che causava il bug.
- `FLAG_NOT_TOUCHABLE` è ciò che rende il velo attraversabile. Di conseguenza
  `MAX_LEVEL` non arriva a 1: un velo opaco su una finestra che non riceve
  tocchi lascerebbe uno schermo nero senza modo di vedere dove premere per
  spegnerlo.
- Il permesso di overlay si concede **solo da una schermata di sistema** e può
  sparire mentre siamo fuori: viene riletto a ogni `onResume`
  (`onOverlayPermissionChanged`), che è anche il punto in cui il velo riparte
  se MIUI ha ucciso il servizio.
- `Screen.DIM` è **escluso da `lockVault`** come `FOCUS`: da lì si apre la
  schermata di sistema del permesso, quindi il launcher passa da `onStop` —
  mandandolo alla home, al ritorno l'utente non vedrebbe sparire l'avviso, che
  è la conferma che il permesso è stato concesso.

Attenuare è l'unica cosa che un overlay sa fare bene: sovrapporre del nero
riduce la luce in modo esatto. **Non aggiungere una tinta qui** — un velo
colorato *aggiunge* luce sul nero e appiattisce il contrasto (§ storia della
modalità rossa, provata e rimossa su richiesta dell'utente).

### Modalità DUMB (`DumbScreen`, `DumbRepository`, `SystemGrayscale`)

Cinque app e basta, per il tempo scelto. Le decisioni di struttura, tutte
prese per una ragione precisa:

- **`dumbActive` non è uno `Screen`, è un ramo prima dell'`AnimatedContent`.**
  `LauncherApp` fa `return` dopo aver composto `DumbScreen`: mentre la modalità
  è attiva non esiste nessun altro composable, quindi non esistono nemmeno i
  rilevatori di gesto di home e cassetto (swipe su, swipe su a due dita,
  doppio tap). Disabilitarli sarebbe stato equivalente sulla carta e diverso
  nei fatti: un gesto che "non fa niente" si scopre in due minuti di dita che
  scorrono per abitudine. Non spostare DUMB dentro l'enum `Screen`.
- **Telefono e messaggi non si salvano.** Sono risolti a runtime da
  `TelecomManager.getDefaultDialerPackage()` e
  `Telephony.Sms.getDefaultSmsPackage()` (nessun permesso, nessuna voce
  `<queries>`: tornano un package, e la componente si ripesca dall'elenco già
  caricato). Salvarli vorrebbe dire che cambiando app predefinita ti ritrovi
  una modalità di autodisciplina che non ti fa più chiamare nessuno.
- **`endsAt` è un orario assoluto su disco**, come per la Concentrazione: con
  un contatore in memoria, per uscire dalla modalità basterebbe aspettare che
  MIUI chiuda il launcher. Il `delay` che chiude la sessione **non scorre a
  telefono addormentato**, quindi `onLauncherResumed` richiama
  `syncDumbExpiry()`.
- **Nessuna icona in `DumbScreen`.** Non è estetica: un'icona è un logo, cioè
  la cosa progettata per farsi notare da mezzo metro. Se qualcuno propone di
  "renderla più usabile" aggiungendole, sta annullando la funzione.
- **Orario di fine, non conto alla rovescia.** Guardare i minuti scendere è
  già stare al telefono, e "mancano 47 minuti" invita a ricontrollare. Per lo
  stesso motivo l'orologio si aggiorna ogni 20 s e non ogni secondo.
- **L'uscita anticipata esiste e viene contata** (`getEarlyExits`). La via
  d'uscita è obbligatoria — l'utente ha già respinto una volta l'idea del
  blocco vero: *"altre app bloccano il telefono realmente ma io voglio essere
  in grado di usarlo comunque se mi serve"*. Il conteggio è la stessa idea
  dello storico della Concentrazione: rendere visibile la ripetizione.
- **`SystemGrayscale` fallisce in silenzio ed è corretto così.** Il grigio su
  tutto il telefono passa dal daltonizzatore di sistema
  (`accessibility_display_daltonizer*` in `Settings.Secure`) e richiede
  `WRITE_SECURE_SETTINGS`, permesso `signature` che si concede solo via ADB.
  Il manifest lo dichiara (con `tools:ignore="ProtectedPermissions"`) perché è
  quello che rende possibile il comando. `dumbGrayscaleAvailable` serve a
  **dirlo** nelle impostazioni: senza, sembrerebbe rotto. Lo stato precedente
  del daltonizzatore viene salvato e ripristinato — qualcuno potrebbe usarlo
  davvero per daltonismo — e il salvataggio avviene solo se non c'è già,
  altrimenti riavviare il launcher a sessione in corso registrerebbe il grigio
  come "stato precedente" e resterebbe grigio per sempre.
- La sessione DUMB **scavalca la conferma della Concentrazione**
  (`launchApp`): le cinque app sono già una decisione presa, una seconda
  conferma sarebbe solo un ostacolo. E `maybeLockHome` non scatta in DUMB: due
  conferme per fare una telefonata.

### Edge-to-edge: la finestra arriva sotto le barre di sistema

`MainActivity` chiama `WindowCompat.setDecorFitsSystemWindows(window, false)`.
Senza, il sistema rimpicciolisce la finestra per stare **fra** la barra di
stato e quella di navigazione, e quelle due strisce non appartengono al
launcher: si vedeva lo sfondo di sistema al posto della griglia, e in DUMB
restavano due bande chiare in cima e in fondo a una schermata che deve essere
tutta spenta. Segnalato dall'utente per entrambe le modalità.

Le regole che ne discendono, da rispettare in ogni schermata nuova:

- **Sfondi e superfici riempiono tutto**, il contenuto toccabile si tiene
  dentro `statusBarsPadding` / `navigationBarsPadding` / `systemBarsPadding`.
  È tutto il punto: `BlurredWallpaperBackground`, il pannello del dock e il
  nero di `DumbScreen` devono arrivare ai bordi fisici.
- Le barre di sistema sono già trasparenti da `themes.xml`; le icone sono
  forzate chiare (`isAppearanceLight*Bars = false`), perché il launcher è
  scuro ovunque e su una ROM in tema chiaro diventerebbero nere su nero.
- **Il padding va alla `Column` che contiene maniglia + `TopAppBar`, e gli
  inset della `TopAppBar` vanno azzerati** (`windowInsets = WindowInsets(0,
  0, 0, 0)`). La `TopAppBar` di Material3 si paga da sola la barra di stato:
  lasciandola fare, la maniglia sopra di lei finisce sotto l'orologio; se si
  paddano entrambe resta un buco alto quanto la barra.
- **In `HomeScreen` il padding sta sulla griglia, non sulla `Column`.** Sembra
  un dettaglio ed è la differenza fra funzionare e no: il tracker dei gesti
  della `Column` confronta `offset.y` (coordinate della `Column`) con
  `dockZoneTop` (coordinate della radice, via `positionInRoot`), e i due
  coincidono solo finché la `Column` parte dove parte la radice. Paddando la
  `Column` si sposterebbero le zone di swipe di tutta l'altezza della barra.
  Il dock si paga la sua barra di navigazione dentro `DockRow(0)`, così il
  pannello traslucido arriva comunque al bordo.

---

## 4. Reattività: cosa è già stato scoperto

Questa è l'area più fragile del progetto e l'utente ci è tornato più volte.
**Prima di ipotizzare, guarda cosa aggiorna lo stato e quanto spesso.**

### Cause reali già trovate e risolte

- **`Modifier.blur()` sullo sfondo del cassetto** — riapplicava l'effetto a
  ogni frame su un layer grande quanto lo schermo. Sostituito con un blur
  software una tantum su una copia piccola (`BlurredWallpaperBackground`).
  **Non reintrodurre `Modifier.blur()`.**
- **Decodifica Drawable→Bitmap dentro il composable** — rifatta a ogni
  ricomposizione/ricreazione di pagina. Ora le icone sono già `Bitmap`,
  decodificate una volta in `AppRepository.loadLaunchableApps()` su
  `Dispatchers.IO` (`ICON_SIZE_PX = 128`). `AppIcon` si limita a `remember { asImageBitmap() }`.
- **`beyondViewportPageCount`** sul pager della home: tolto, costava e non
  serviva.
- **Il countdown della sessione dentro `uiState`** — `LauncherApp` raccoglie
  `uiState` alla radice, quindi un campo aggiornato ogni secondo ricompone
  **tutto l'albero**, cassetto compreso (che rifiltra l'elenco completo delle
  app installate). Per questo `focusRemainingSeconds` è un `StateFlow`
  separato, raccolto **dentro** `FocusPill` e `RunningSession` con
  `collectAsState()`. In `uiState` resta solo `focusActive: Boolean`, che
  cambia due volte per sessione.
  **Regola generale: niente che ticchetti dentro `uiState`.** Lo storico della
  Concentrazione (`focusBreaks`, `focusRecordSeconds`, `frictionStreakSeconds`)
  sta invece dentro `uiState` senza problemi: cambia solo quando cedi o quando
  una sessione inizia o finisce. Il "stai resistendo da X" del popup è una
  fotografia presa all'apertura, non un contatore.

### Cose provate che NON hanno risolto (build 21–23, poi rimosse)

Se ti viene in mente una di queste, sappi che è già stata provata e scartata:

- memoizzare `state.homePages`, `state.dockSlots` e la lambda `isMuted` in
  `HomeScreen` con `remember(...)`;
- memoizzare `state.visibleApps` / `state.hiddenApps` in `DrawerScreen`;
- riusare le `AppInfo` già decodificate in `refreshApps()` per non ridecodificare
  le icone a ogni `onResume`;
- spostare la pill in un overlay posizionato con `Modifier.offset { }` sul
  `dockZoneTop` misurato.

Non sono sbagliate in sé, ma non hanno recuperato la reattività e hanno reso
il diff difficile da bisezionare. La 1.0.24 le ha tolte tutte.

### App in grigio: servono due segnali, non uno

Togliere la saturazione a un'icona già bianca, nera o grigia **non cambia un
pixel**, e non si capiva quali app fossero bloccate durante una sessione. Per
questo `AppIcon` applica anche `MUTED_ALPHA`: la trasparenza agisce sul
rapporto con lo sfondo, non sui colori dell'icona, quindi funziona sempre.
Il parametro `faded` è separato da `grayscale` solo per la conferma di
apertura, dove l'icona è il soggetto e sbiadirla la renderebbe difficile da
riconoscere.

### Leggibilità dei popup

I due popup (scelta della durata, conferma di apertura) condividono i colori in
`PromptStyle.kt`. La carta è **scura e piena**, non un velo traslucido: il velo
si leggeva sulla home ma non sopra il cassetto, che è una griglia di icone
colorate. Non "quasi piena": con un filo di trasparenza il contrasto torna a
dipendere da cosa c'è dietro, ed è già stato segnalato due volte.

**Verificare la versione installata**: in fondo alle impostazioni delle app
nascoste c'è `Hidden Layer 1.0.x`. Prima di indagare una segnalazione che
sembra già corretta, chiedere quel numero: due giri sono stati spesi su uno
screenshot che mostrava una build precedente alla correzione.

### Trovate da un audit del codice (1.0.29)

- **Sfondo sfocato ricalcolato a ogni apertura** di cassetto/Concentrazione:
  `produceState` senza chiavi ripartiva a ogni ingresso in composizione e
  ogni giro allocava una bitmap grande quanto lo schermo (~10 MB) solo per
  ridurla a 64 px. Ora la copia sfocata è in cache per tutto il processo
  (`cachedBlur`); un cambio di sfondo si vede al riavvio del launcher.
- **`EncryptedSharedPreferences` costruito nel costruttore del ViewModel**,
  sul thread principale e senza `try/catch`: se l'archivio si corrompe o la
  chiave del Keystore viene invalidata, `MainActivity` crasha a ogni avvio e
  il telefono resta senza home da cui disinstallare. Ora è pigro, su IO, e
  degrada a "nessuna app nascosta" invece di far cadere tutto.

### Sospetto rimasto sul doppio tap

`detectTapGestures(onDoubleTap = ...)` su `HomePage` **ritarda il
riconoscimento del tocco singolo** di tutta la finestra del doppio tap. Sulla
home non c'è un `onTap` in quel detector, quindi in teoria non si nota, e dalla
1.0.24 alla 1.0.33 l'utente non ha più segnalato lentezza — il sospetto è
molto più debole, ma non è mai stato verificato in isolamento.

Se la lentezza torna **anche senza mai avviare una sessione**, è ancora il
primo posto da guardare, e la soluzione è spostare la scorciatoia su un gesto
che non tocchi il riconoscimento dei tap normali.

### Come far diagnosticare all'utente

Le tre domande che discriminano:

1. lento **sempre**, anche senza mai avviare una sessione? → gesture / layout;
2. lento **solo con una sessione attiva**? → qualcosa ticchetta nello stato;
3. lento **subito dopo essere tornato da un'app**? → `onResume` →
   `refreshApps()`, che rilegge tutte le app e ridecodifica tutte le icone.

### La home chiusa dopo lo sblocco (`HomeLockOverlay`)

Durante la Concentrazione, il primo ritorno alla home dopo uno sblocco vero
la trova chiusa: si riapre **solo** chiedendo il telefono.

- **Un'attesa passiva non funziona, ed è già stata provata e scartata.** La
  prima versione era un respiro di N secondi che si apriva da solo, con la
  durata che cresceva a ogni sblocco. L'utente l'ha bocciata: *"aspettare non
  causa che io non prenda il telefono"* — un ritardo rimanda l'impulso invece
  di interromperlo, perché guardi lo schermo pensando già all'app che volevi.
  Ora la home resta chiusa finché non tocchi "Mi serve il telefono", che è una
  **decisione** invece che un'attesa. Non riproporre il timer.
- **La via d'uscita deve restare sempre disponibile.** Richiesta esplicita:
  *"altre app che bloccano il telefono lo bloccano realmente ma io voglio
  essere in grado di usarlo comunque se mi serve"*. Il costo è il gesto
  intenzionale e il conteggio visibile (`homeLockRequests`, mostrato dalla
  seconda volta in poi), non l'impossibilità di passare.
- **`onResume` e `ACTION_USER_PRESENT` arrivano in ordine imprevedibile, e il
  primo tentativo dava per scontato il contrario.** Sbloccando, il launcher fa
  `onResume` **dietro** la schermata di blocco — appena lo schermo si accende
  — e il broadcast arriva solo dopo, a blocco tolto. Controllando il flag
  dentro `onResume` non era ancora alzato, restava buono, e il blocco
  compariva al primo `onResume` successivo: bug reale, l'utente lo vedeva
  premendo il tasto home dalla schermata Concentrazione invece che sbloccando.
  Ora `maybeLockHome()` è chiamata **da entrambi i lati** e scatta chi arriva
  per ultimo; `unlockedAtElapsed` (timestamp, non booleano) fa scartare uno
  sblocco più vecchio di `HOME_LOCK_GRACE_MILLIS`, che è il caso "hai
  sbloccato dentro un'altra app e torni alla home molto dopo".
- Serve anche `onLauncherPaused()` da `MainActivity.onPause`: senza sapere se
  il launcher è davvero in primo piano, il blocco scatterebbe alle sue spalle
  mentre sei in un'altra app.
- `ACTION_USER_PRESENT` non è mai stato consegnabile a un receiver dichiarato
  nel manifest, nemmeno prima delle restrizioni sui broadcast impliciti di
  Android 8: va registrato a runtime (`ContextCompat.registerReceiver` con
  `RECEIVER_NOT_EXPORTED`, nell'`init` del ViewModel) e disiscritto in
  `onCleared()`. Vive per tutta la vita del processo, non solo mentre
  l'activity è in primo piano — il launcher è quasi sempre vivo, essendo la
  home.
- Scatta **solo se `focusActive`**: fuori da una sessione non succede niente.
  Non è un freno sempre acceso — l'utente lo vuole legato alla Concentrazione
  (chiesto esplicitamente). E `endSession` azzera `homeLockActive`: se la
  sessione scade a home chiusa, altrimenti resteresti davanti a una schermata
  che chiede il telefono per una sessione che non esiste più.
- `homeLockActive` sta in `uiState` (come `focusToastVisible`): cambia due
  volte per attivazione, non ticchetta.
- **È un `Dialog`, non un `Box` dentro `LauncherApp`.** All'epoca la finestra
  del launcher non arrivava sotto la barra di stato e quella di navigazione,
  quindi un overlay in composizione lasciava scoperte due strisce (segnalato
  con screenshot). Servono **due** cose insieme: `FLAG_LAYOUT_NO_LIMITS` **e**
  `layoutInDisplayCutoutMode` — con il solo primo il sistema tiene comunque la
  finestra sotto il notch, e restava scoperto il bordo superiore mentre quello
  inferiore era a posto (secondo screenshot). Da quando il launcher è
  edge-to-edge (§ sotto) un `Box` basterebbe, ma resta un `Dialog`: essendo una
  finestra a sé blocca anche i tocchi alla griglia sotto, senza doverla
  disabilitare a mano.
- **L'apertura è una macchia che si allarga** (`BlendMode.Clear` su un
  `CompositingStrategy.Offscreen`, con un gradiente radiale per il bordo
  sfumato): richiesta esplicita dell'utente, al posto dell'anello di
  avanzamento che c'era prima. `Clear` ha bisogno del layer proprio,
  altrimenti cancella anche ciò che sta sotto nel buffer.
- Il conteggio sta in `FocusRepository` (non in RAM) perché una sessione dura
  ore e MIUI chiude volentieri il launcher; si azzera in `startFocus`,
  altrimenti la seconda sessione della giornata partirebbe col conto della
  prima.
- **Niente scorciatoie per uscire**: `dismissOnBackPress` e
  `dismissOnClickOutside` a false. Un modo per saltare la richiesta sarebbe un
  modo per non farla mai.

---

## 5. Gesture: le regole imparate a caro prezzo

Compose consegna gli eventi ai **figli prima che ai genitori** nel pass `Main`,
e ai **genitori prima che ai figli** nel pass `Initial`. Quasi tutti i bug di
gesture di questo progetto venivano da lì.

- **Mai due detector che competono sullo stesso elemento.** Un
  `detectTapGestures` che consuma il down aborta un `awaitLongPressOrCancellation`
  vicino, e il long press fallito cade sul gestore sottostante (era il bug per
  cui tenendo premuta un'icona usciva il menu "Home" sbagliato).
- Le tile hanno **un solo `combinedClickable`** (tap + long click). Il
  trascinamento tra pagine è seguito **a livello di `HomeScreen`** sul pass
  `Initial`, che arriva al genitore prima che un figlio possa consumare.
- **Le app nascoste si aprono solo con lo swipe su a due dita dalla home**
  (tracker sul pass `Initial` in `HomeScreen`, `SECRET_SWIPE_THRESHOLD`), che
  chiama `openHiddenDrawer()` → `Screen.HIDDEN_DRAWER`. L'ingresso è passato per
  tre forme, e la direzione è sempre la stessa — **togliere ciò che si vede**:
  1. swipe a sinistra su un pager: la pagina si affacciava già durante il
     trascinamento, bastava una scorsa per scoprirla;
  2. doppio tap su due puntini in fondo al cassetto (`HiddenDoorDots`): niente
     scorrimento laterale, ma un elemento disegnato prima o poi viene toccato;
  3. gesto a due dita: non lascia niente sullo schermo, ed è **un gesto solo
     dalla home** invece di cassetto → puntini → doppio tap.

  Conseguenze in `DrawerScreen`: cassetto e nascoste non sono più due stati
  scambiabili ma **la stessa schermata in due versioni**, decisa dal parametro
  `hiddenDrawer` e mai scambiata mentre sei dentro. Niente `showHidden`, niente
  crossfade fra le due: `FADE_MILLIS` serve solo fra sblocco e app nascoste.
  Dalle nascoste si esce **alla home** con X, trascinamento in giù o tasto
  indietro — prima l'unica uscita era il tasto indietro di sistema, che per una
  schermata aperta con un gesto non si trova.
- **Le due versioni sono due valori di `Screen`** (`DRAWER` e `HIDDEN_DRAWER`),
  non un campo dello stato letto all'ingresso con `remember`. `AnimatedContent`
  alla radice tiene la schermata uscente **composta** finché la sua animazione
  non finisce: con un solo `Screen.DRAWER`, chiudere il cassetto e riaprire
  subito quello nascosto ricadeva su quella composizione ancora viva, il
  `remember` non veniva rivalutato e si riapriva il cassetto normale. Vale come
  regola: **se due schermate devono essere composte da zero, devono essere due
  voci diverse per `AnimatedContent`** — un flag dentro lo stato non basta.
- **Il gesto nasconde la porta, non la chiude a chiave.** Contro chi ha il
  telefono in mano vale solo lo sblocco (`unlockRequired`). Per questo uscendo
  dalle nascoste si chiama `relockVault()`: `lockVault()` da solo scatta a
  `onStop`, quindi chi prendeva il telefono nei secondi dopo — schermo acceso,
  launcher in primo piano — rientrava senza impronta. Attenzione all'ordine
  quando si tocca quel punto: se `locked` tornasse vero mentre la schermata è
  ancora quella nascosta, `LaunchedEffect` rilancerebbe il prompt biometrico.
- **La home assorbe il tasto indietro** (`BackHandler` in `HomeScreen`). Un
  launcher è la radice del suo task: lasciando passare l'indietro, l'activity
  finisce e il sistema la riapre subito perché è la home, quindi si ripassa da
  `onResume` → `refreshApps()` — tutte le app rilette e tutte le icone
  ridecodificate. Da fuori si vede il dock che si ridisegna a vuoto. Se le file
  del dock sono aperte le richiude, altrimenti non fa niente. I popup hanno i
  loro `BackHandler` e sono composti **dopo**, quindi mantengono la precedenza
  (vince l'ultimo registrato).
- Lo swipe-giù-per-chiudere di cassetto e Concentrazione osserva il pass
  `Initial` sulla radice della schermata e **non consuma mai** (`CloseGestures.kt`).
  Metterlo sulla `TopAppBar` non funzionava: il campo di ricerca la copre.
- Zone in conflitto sulla home, tenute separate per coordinate misurate:
  - primi `TOP_GESTURE_EXCLUSION` (56dp) dall'alto → lasciati al sistema
    (tendina notifiche);
  - sopra `dockZoneTop` → swipe su apre il **cassetto**;
  - sul dock → swipe su apre le **file nascoste del dock**;
  - **due dita**, ovunque sotto la striscia in alto → app nascoste. Distinguere
    un dito da due è l'unico modo di far convivere due gesti identici: il
    tracker sulla radice conta `event.changes.count { it.pressed }`, alza
    `multiTouch` e consuma, e lo swipe a un dito nel `Column` si tira indietro
    leggendo quel flag. Funziona perché il pass `Initial` del genitore precede
    il pass `Main` del figlio **nello stesso evento**.
- **File del dock aperte**: il primo gesto che parte fuori dal dock le
  richiude e basta (consumato sul pass `Initial` nel tracker di `HomeScreen`,
  variabile `dismissingDock`). Non è un velo cliccabile a parte apposta: un
  secondo detector sopra la griglia si contenderebbe i tocchi con le tile.
- **Slot vuoto del dock**: sia il tap sia il tocco lungo aprono il selettore
  (`DockRow`, parametro `onEmptySlot`). Prima rispondeva solo al tocco lungo,
  ma l'icona "+" disegnata lì suggerisce un tap — l'unico gesto che
  l'interfaccia promette era anche l'unico che non faceva niente. E dal
  cassetto (`MenuOrigin.DRAWER`) il menu contestuale ha anche "Aggiungi al
  dock", non solo "Aggiungi alla home": prima l'unica strada per il dock era
  aggiungere l'app alla home e poi tenerla premuta di nuovo lì.

---

## 6. Costanti che definiscono il layout

| Dove | Costante | Valore |
|---|---|---|
| `HomeLayoutRepository` | `DOCK_COLUMNS` / `DOCK_ROWS` / `DOCK_SIZE` | 5 / 3 / 15 |
| `HomeScreen` | `HOME_COLUMNS` | 4 |
| `HomeScreen` | `HOME_TILE_HEIGHT` | 52dp (senza etichetta) |
| `HomeScreen` | `PAGE_PADDING_*` / `ROW_SPACING` / `COLUMN_SPACING` | 30·44dp / 26dp / 18dp |
| `HomeScreen` | `TOP_GESTURE_EXCLUSION` | 56dp |
| `HomeScreen` | `PAGE_MOVE_THRESHOLD` / `TAP_VS_DRAG_THRESHOLD` | 72dp / 16dp |
| `HomeScreen` | `DOCK_TOGGLE_THRESHOLD` | 28dp |
| `HomeScreen` | `SECRET_SWIPE_THRESHOLD` (due dita → nascoste) | 48dp |
| `DrawerScreen.AppGrid` | colonne | 5 |
| `AppGridTile` / `HomeIconTile` | icona | 48dp |
| `AppIcon` | `MUTED_ALPHA` (app in grigio) | 0.4 |
| `PromptStyle` | superficie dei fogli | `HlSurface` `#1C1913`, opaca |
| `Tokens.kt` | margine di schermata (Carta / Indice) | 26dp / 34dp |
| `Tokens.kt` | area di tocco minima delle azioni testuali | 44dp |
| `AppRepository` | `ICON_SIZE_PX` | 128 |
| `FocusRepository` | `PRESET_MINUTES` | 15/30/45/60/120 |
| `FocusRepository` | `SHORTCUT_MINUTES` | 30/60/120 (il popup) |
| `LauncherViewModel` | `FOCUS_TOAST_MILLIS` | 5000 |

**Le pagine della home non hanno un numero fisso di righe**: `HomeScreen`
misura l'altezza reale e riporta `pageSize` al ViewModel. Cambiare
`HOME_TILE_HEIGHT` cambia quante app stanno in una pagina.

---

## 6-bis. Il sistema visivo (redesign minimalista)

Il linguaggio dell'interfaccia sta in `ui/theme/Tokens.kt` (colori, forme,
misure), `ui/theme/Theme.kt` (schema scuro fisso + `Typography`) e
`ui/Minimal.kt` (i pochi componenti riusati ovunque). **Prima di scrivere una
schermata nuova, guarda lì**: quasi tutto quello che serve esiste già.

Le regole, e il perché — sono tutte funzionali al detox, non estetiche:

- **Un solo accento** (`HlPaper`, bianco sporco caldo). Nessun colore saturo
  in nessuna schermata: il colore è ciò che fa girare la testa verso lo
  schermo. Un errore o un permesso mancante si dicono con un'etichetta in
  maiuscoletto, non con il rosso (`DimScreen` aveva un `#FFB4A3`, tolto).
- **Il nero è caldo** (`#14120F`). Un nero neutro su OLED al buio vira
  all'azzurro e si legge come "schermo acceso".
- **Niente vetro smerigliato.** La ricerca e la pill della Concentrazione
  erano capsule bianche al 15%: le due cose più luminose del cassetto e della
  home. Ora sono, rispettivamente, una riga con un filetto sotto e una riga di
  testo in monospazio.
- **Niente chrome di Material**: `TopAppBar`, `Scaffold`, `ListItem`,
  `Switch`, `AlertDialog`, `OutlinedTextField` sono spariti da tutto il
  progetto (resta solo `HorizontalDivider`, dentro `Hairline`). Portavano
  altezze fisse, ripple, contenitori squadrati e un titolo in corpo grande per
  dire quello che ora dicono due parole in maiuscoletto.
- **Le icone sono quasi tutte diventate testo.** `Close` → "Chiudi",
  `Settings` → "Impostazioni", `Add` del dock → contorno tratteggiato,
  `Fingerprint` → "Impronta". Restano `Search` e `VisibilityOff` nei campi di
  ricerca. Ogni icona è una forma che l'occhio prende prima del testo, ed è
  esattamente ciò che qui va ridotto.
- **I popup sono fogli in basso** (`PromptSheet`), allineati a sinistra: una
  carta centrata è una finestra di sistema, e mette i pulsanti lontano dal
  pollice.
- **I numeri sono in monospazio** (`MonoValue`): conteggi, record, countdown,
  classifiche. Le cifre non cambiano larghezza mentre scorrono, quindi la riga
  non balla e una classifica si legge come una tabella.
- **Ogni azione diventata testuale conserva 44dp di area di tocco**
  (`HlTouchTarget`, usato da `TextAction`, `MinimalSwitch`, `FocusPill`). È il
  modo tipico in cui questo tipo di interfaccia si rompe: si alleggerisce la
  grafica e restano bersagli da 16dp.

Trappole già pagate in questo redesign:

- **`HiddenManagerScreen` disegna la carta riga per riga**, arrotondando solo
  la prima e l'ultima del gruppo. Raccogliere le righe dentro un unico `item`
  per avere un solo contenitore comporrebbe duecento righe con le loro icone
  in un colpo all'apertura — la lista deve restare pigra.
- **`FocusScreen`, sessione in corso**: la durata del tratto di resistenza si
  ricava da `focusStreakStartMillis` (un **istante**, che cambia solo quando
  cedi) sottraendo l'ora corrente nella composizione, che sta già ricomponendo
  per il countdown. Un campo con la durata dentro `uiState` ticchetterebbe una
  volta al secondo e farebbe ricomporre tutta l'app — vedi §4.

---

## 7. Privacy: la regola che si dimentica

Le app nascoste non devono comparire in **nessuna** schermata non protetta.
Un audit ne ha trovate quattro violazioni tutte insieme, quindi non è una
regola che si applica da sola: l'elenco della Concentrazione le mostrava per
nome, `lockVault` non azzerava menu contestuale e conferma di apertura né
riportava alla home dal cassetto, e `FLAG_SECURE` si spegneva durante la
transizione fra due schermate protette (ora è **contato**, non acceso e
spento da ciascuna per conto suo).
Vale anche per funzioni che sembrano non c'entrare: lo storico della
Concentrazione le esclude sia in scrittura (`launchAnyway`) sia in lettura
(`FocusHistory`), perché altrimenti il nome di un'app nascosta finirebbe in
chiaro nella classifica dentro le impostazioni. Prima di aggiungere qualunque
cosa che elenchi app per nome, chiediti da dove si raggiunge.

**Bloccare un'app nascosta durante la Concentrazione** era raggiungibile solo
mostrandola, andando in `FocusScreen` a selezionarla, e nascondendola di
nuovo — perché `FocusScreen` esclude di proposito le app nascoste dal suo
elenco (si apre con un doppio tap, senza sblocco). La correzione **non** è
stata togliere quel filtro: è stata aggiungere l'azione "Blocca durante la
Concentrazione" al menu contestuale del cassetto nascosto stesso
(`MenuOrigin.HIDDEN_DRAWER`), che è già dietro lo sblocco quando è attivo.
Riusa `toggleFocusApp`, la stessa funzione di `FocusScreen` — non è vincolata
a quella schermata. La regola resta identica, cambia solo da dove si agisce:
il nome dell'app non deve mai comparire in una schermata **non protetta**,
ma può benissimo comparire — e ricevere azioni — in una che lo è già.

---

## 8. Limiti dichiarati (non riproporli come soluzioni)

Un launcher senza root **non può**: importare il layout di un altro launcher;
nascondere le app da Impostazioni, ricerca globale MIUI, Play Store, notifiche
e statistiche; **impedire davvero** l'apertura di un'app (senza un servizio di
accessibilità). Per un blocco vero servono la Modalità concentrazione o il
Secondo Spazio di MIUI. Tutto questo è già stato spiegato all'utente più volte.

**Le notifiche scavalcano la conferma d'apertura**, ed è lo stesso limite in
un'altra forma: il tap su una notifica porta un `PendingIntent`, un token
opaco che il sistema consegna diretto all'app di destinazione. Nessun'altra
app — nemmeno con `BIND_NOTIFICATION_LISTENER_SERVICE` — può leggerne il
contenuto o sostituirlo: è il punto stesso per cui i `PendingIntent`
esistono. L'unica cosa fattibile sarebbe cancellare la notifica vera e
ripubblicarne una copia nostra — ma cambierebbe il mittente visibile
(risulterebbe da Hidden Layer, non dall'app originale) e perderebbe la
destinazione specifica (riaprirebbe l'app in generale, non la chat/schermata
esatta a cui puntava). Proposto e **rifiutato dall'utente**: non
riproporlo come soluzione, il compromesso non vale il risultato. L'utente ha
scelto di lasciare il buco documentato invece che tapparlo con qualcosa di
fragile.

Mancano di proposito: widget (`AppWidgetHost` è un sottosistema a parte),
riordino drag & drop **dentro** la stessa pagina, cartelle, badge di notifica
(assenti per scelta: contraddirebbero la Concentrazione).

---

## 9. Convenzioni

- **Lingua**: interfaccia e `README.md` in italiano; commenti nel codice in
  italiano per le parti nuove, inglese dove già c'era — non riscrivere i
  commenti inglesi esistenti solo per uniformare.
- I commenti spiegano **perché**, non cosa. Molti codificano un bug già
  risolto: leggili prima di "semplificare", spesso sono l'unica traccia.
- Non aprire pull request se l'utente non lo chiede.
- L'utente non può ricevere l'APK da qui: gli si passa il link al run.
