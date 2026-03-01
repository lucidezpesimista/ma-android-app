# Estructura del Proyecto Ma

## Resumen de Archivos Creados

### Configuración del Proyecto
```
├── build.gradle.kts                 # Build script raíz
├── settings.gradle.kts              # Configuración de settings
├── gradle.properties                # Propiedades de Gradle
├── gradlew                          # Gradle wrapper (Unix)
├── gradlew.bat                      # Gradle wrapper (Windows)
├── gradle/wrapper/gradle-wrapper.properties
├── setup.sh                         # Script de configuración inicial
├── .gitignore                       # Archivos ignorados por git
└── README.md                        # Documentación principal
```

### Módulo App
```
app/
├── build.gradle.kts                 # Dependencias y configuración
├── proguard-rules.pro               # Reglas de ProGuard
└── src/
    ├── main/
    │   ├── AndroidManifest.xml      # Manifest de la app
    │   ├── java/com/ma/app/
    │   │   ├── MainActivity.kt      # Activity principal
    │   │   ├── MaApplication.kt     # Application class
    │   │   ├── data/
    │   │   │   ├── database/
    │   │   │   │   ├── AppDatabase.kt      # Database Room
    │   │   │   │   ├── Converters.kt       # TypeConverters
    │   │   │   │   └── NodeDao.kt          # DAO
    │   │   │   ├── model/
    │   │   │   │   └── Node.kt             # Entidad Node
    │   │   │   └── repository/
    │   │   │       └── NodeRepository.kt   # Repository
    │   │   ├── ui/
    │   │   │   ├── components/
    │   │   │   │   ├── Breadcrumb.kt       # Navegación breadcrumb
    │   │   │   │   └── OutlineItem.kt      # Item de lista
    │   │   │   ├── navigation/
    │   │   │   │   └── Navigation.kt       # Grafo de navegación
    │   │   │   ├── screens/
    │   │   │   │   ├── OutlineScreen.kt    # Pantalla principal
    │   │   │   │   ├── SearchScreen.kt     # Pantalla de búsqueda
    │   │   │   │   ├── NodeDetailScreen.kt # Detalle de nodo
    │   │   │   │   └── SettingsScreen.kt   # Configuración
    │   │   │   └── theme/
    │   │   │       ├── Color.kt            # Colores
    │   │   │       ├── Theme.kt            # Tema Material 3
    │   │   │       └── Type.kt             # Tipografía
    │   │   ├── utils/
    │   │   │   ├── TextParser.kt           # Parser hashtags/links
    │   │   │   └── MarkdownExporter.kt     # Export/import MD
    │   │   └── viewmodel/
    │   │       ├── OutlineViewModel.kt     # VM del outline
    │   │       ├── SearchViewModel.kt      # VM de búsqueda
    │   │       ├── NodeDetailViewModel.kt  # VM de detalle
    │   │       └── ExportViewModel.kt      # VM de exportación
    │   └── res/
    │       ├── drawable/
    │       │   ├── ic_launcher_background.xml
    │       │   └── ic_launcher_foreground.xml
    │       ├── mipmap-anydpi-v26/
    │       │   ├── ic_launcher.xml
    │       │   └── ic_launcher_round.xml
    │       ├── values/
    │       │   ├── colors.xml
    │       │   ├── strings.xml
    │       │   └── themes.xml
    │       └── xml/
    │           ├── backup_rules.xml
    │           └── data_extraction_rules.xml
    └── test/
        └── java/com/ma/app/
            ├── TextParserTest.kt      # Tests del parser
            └── TreeOperationsTest.kt  # Tests de operaciones de árbol
```

## Decisiones Técnicas Clave

### 1. Fractional Indexing (orderIndex: Double)
- Permite insertar entre ítems sin reindexar toda la lista
- Ejemplo: entre 1.0 y 2.0 insertamos 1.5
- Reindexado masivo solo cuando el espacio es < 0.0001

### 2. Focus mediante currentNodeId
- `null` = raíz (nodos sin padre)
- `Long` = enfocado en ese nodo (mostrar solo sus hijos)
- Breadcrumb construido dinámicamente desde ancestros

### 3. Hashtags y Links como texto plano
- Regex para detectar `#tag` y `[[link]]`
- No tabla normalizada para MVP
- Computados al vuelo

### 4. Arquitectura MVVM + Repository
- ViewModels con StateFlow para UI reactiva
- Repository con Coroutines + Flow
- Operaciones de DB en IO dispatcher

### 5. Gestos de baja fricción
- Swipe derecha = indent
- Swipe izquierda = outdent
- Tap en texto = editar inline
- Tap en chevron = focus

## Cómo Empezar

1. **Opción A - Android Studio (Recomendado):**
   ```bash
   # Ejecuta el script de setup
   ./setup.sh
   
   # Abre Android Studio
   # File > Open > Selecciona esta carpeta
   # Sync Project with Gradle Files
   # Run > Run 'app'
   ```

2. **Opción B - Línea de comandos:**
   ```bash
   ./setup.sh
   ./gradlew assembleDebug
   # Instala el APK generado en app/build/outputs/apk/debug/
   ```

## Próximos Pasos Sugeridos

1. **Drag & Drop**: Implementar reordenamiento visual
2. **Atajos de teclado**: Para tablets y Chromebooks
3. **Widgets**: Acceso rápido desde home screen
4. **Sincronización**: Opcional, si se necesita multi-dispositivo
5. **Temas**: Modo oscuro automático
