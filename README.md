# IPTV Pro — Android TV / Fire TV

Aplicación IPTV para Fire TV y Android TV. Reproduce listas M3U propias con interfaz Netflix-like, EPG/XMLTV, favoritos e historial por lista.

---

## Stack técnico

| Componente | Librería |
|---|---|
| UI | Jetpack Compose + Material3 |
| Player | Media3 / ExoPlayer 1.3.0 |
| Persistencia | Room 2.6.1 |
| DI | Hilt 2.51 |
| Imágenes | Coil 2.6.0 |
| Navegación | Navigation Compose 2.7.7 |
| Async | Coroutines + StateFlow |

---

## Estructura del proyecto

```
app/src/main/java/com/iptvpro/app/
├── data/
│   ├── db/            ← Room: entidades, DAOs, AppDatabase
│   ├── parser/        ← M3uParser, XmltvParser
│   ├── remote/        ← RemoteDataSource (HTTP)
│   ├── repository/    ← PlaylistRepository, ChannelRepository, EpgRepository
│   └── util/          ← TextUtils (normalización)
├── domain/
│   └── model/         ← Playlist, Channel, ChannelGroup, EpgProgram, NowNextInfo
├── di/                ← DatabaseModule (Hilt)
└── ui/
    ├── theme/         ← Color, Theme, Type (10-foot TV)
    ├── navigation/    ← Screen, AppNavigation
    ├── components/    ← ChannelCard, GroupCard, NowNextBadge, LoadingScreen, ErrorScreen
    └── screens/
        ├── playlists/ ← Gestión de listas M3U
        ├── home/      ← Home de lista (favoritos + recientes + categorías)
        ├── channels/  ← Canales de una categoría
        ├── search/    ← Buscador de canales
        └── player/    ← PlayerActivity + PlayerViewModel
```

---

## Instrucciones de compilación

### Requisitos

- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17
- Android SDK 34
- Gradle 8.6

### Pasos

1. **Clonar el repo** y abrirlo en Android Studio:
   ```bash
   git clone <repo-url>
   ```

2. **Configurar `local.properties`** con la ruta de tu SDK:
   ```
   sdk.dir=/home/<user>/Android/Sdk
   ```

3. **Sincronizar Gradle** (File → Sync Project with Gradle Files)

4. **Build debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   La APK estará en `app/build/outputs/apk/debug/app-debug.apk`

5. **Build release APK** (necesita keystore configurado):
   ```bash
   ./gradlew assembleRelease
   ```

---

## Instalación en Fire TV / Android TV

### Método ADB WiFi (recomendado)

1. En tu Fire TV: *Ajustes → Mi Fire TV → Opciones para desarrolladores → Depuración ADB: ON*
2. Conectar al mismo WiFi que tu PC
3. Obtener la IP del Fire TV (Ajustes → Mi Fire TV → Acerca de → Red)
4. En tu PC:
   ```bash
   adb connect <IP_FIRE_TV>:5555
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
5. La app aparecerá en el launcher de Fire TV en "Tus aplicaciones y canales"

### Método USB

1. Conectar Fire TV con cable USB al PC
2. Activar depuración ADB (mismo pasos que arriba)
3. `adb install -r app-debug.apk`

---

## Funcionalidades implementadas

- [x] Pantalla gestión de múltiples listas M3U (agregar/editar/eliminar)
- [x] URL EPG por lista (manual + auto-detección desde cabecera EXTM3U)
- [x] Parser M3U robusto (nombre, URL, group-title, tvg-logo, tvg-id, tvg-name)
- [x] Parser XMLTV incremental (XmlPullParser, sin cargar en RAM)
- [x] Aislamiento completo por lista (canales, favoritos, historial, EPG)
- [x] Home TV: fila favoritos + fila recientes + categorías alfabéticas
- [x] Pantalla de canales por categoría (orden alfabético)
- [x] Buscador de canales (debounce 250ms, búsqueda normalizada)
- [x] Logos de canal (Coil, placeholder con iniciales si falta logo)
- [x] EPG Now/Next en tarjetas y reproductor
- [x] Reproductor Media3 con gestión de audio focus y lifecycle
- [x] Historial: últimos 20 vistos, sin duplicados (actualiza timestamp)
- [x] Favoritos por lista: toggle desde tarjeta y reproductor
- [x] Estados: cargando / vacío / error / éxito en todas las pantallas
- [x] Navegación D-pad: focus con escala y borde blanco
- [x] Compatibilidad Fire TV (LEANBACK_LAUNCHER, sin touchscreen requerido)

---

## Mejoras futuras (v2/v3)

- [ ] Guía TV completa por franja horaria (EPG grid)
- [ ] Caché local de logos (Coil disk cache ya incluido, configurar límite)
- [ ] Refresco automático de lista y EPG (WorkManager cada X horas)
- [ ] Continuar viendo (guardar posición de reproducción)
- [ ] Búsqueda por voz (Android TV voice search intent)
- [ ] Filtros rápidos por grupo dentro del buscador
- [ ] MultiEPG (ver guía de varios canales a la vez)
- [ ] Selector de ordenación (por número de canal, por favoritos primero)
- [ ] Tests unitarios: parser M3U, parser XMLTV, repositorios
- [ ] Tests de navegación con D-pad (Compose UI tests)
- [ ] Soporte Chromecast / Cast SDK

---

## QA Checklist

### Funcional
- [ ] Agregar lista con URL válida → sincroniza y muestra canales
- [ ] Agregar lista con URL inválida → muestra error, no guarda
- [ ] Editar lista → actualiza datos y re-sincroniza
- [ ] Eliminar lista → confirma y elimina con todos sus datos
- [ ] Favorito desde tarjeta → aparece en fila de favoritos del home
- [ ] Favorito desde reproductor → estado persistido tras cerrar app
- [ ] Historial → últimos 20, el más reciente primero, sin duplicados
- [ ] Búsqueda → resultados inmediatos (debounce), navegar con D-pad
- [ ] EPG → Now/Next en tarjetas cuando hay EPG disponible
- [ ] Sin EPG → app funciona igual sin bloqueos
- [ ] Canal sin logo → muestra placeholder con iniciales

### Mando / Focus
- [ ] D-pad navega entre filas de home correctamente
- [ ] Dentro de LazyRow: izquierda/derecha mueve entre tarjetas
- [ ] Back desde canales → vuelve al home de la lista
- [ ] Back desde home → vuelve a lista de playlists
- [ ] Back desde reproductor → vuelve a la pantalla anterior
- [ ] Buscador recibe foco automáticamente al entrar
- [ ] Focus siempre visible (borde blanco + escala)

### Rendimiento
- [ ] Lista de 10.000+ canales → UI no se bloquea (parsing en IO thread)
- [ ] Búsqueda en lista grande → resultados en <500ms
- [ ] EPG grande (>100MB XML) → parse incremental sin OOM

### Red / Errores
- [ ] URL M3U caída → error claro + botón Reintentar
- [ ] Canal con stream inválido → error en reproductor + botón Reintentar
- [ ] XMLTV malformado → app continúa sin EPG, sin crash
- [ ] Sin conexión de red → error descriptivo en sincronización
