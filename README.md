# 間 (ma) - Outliner Personal

Un outliner jerárquico ultra-rápido para Android, inspirado en WorkFlowy. Diseñado para uso personal, offline-first, con énfasis en baja fricción y fluidez.

## Características

- **Árbol jerárquico infinito**: Cada ítem puede tener hijos, que a su vez pueden tener más hijos
- **Focus por rama**: Entra en "focus mode" para ver solo una rama del árbol
- **Búsqueda con contexto**: Busca en todo el árbol y ve los resultados con su jerarquía completa
- **Hashtags clickeables**: Escribe `#tag` en cualquier texto y toca para filtrar
- **Links internos**: Usa `[[Nombre del Nodo]]` para crear links entre nodos
- **Edición inline**: Edita títulos directamente en la lista, sin pantallas intermedias
- **Gestos rápidos**: Swipe derecha para indentar, izquierda para desindentar
- **Exportación a Markdown**: Exporta tu árbol completo o solo una rama
- **Backup/Restore**: Guarda y restaura tu datos con Storage Access Framework

## Stack Tecnológico

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Persistencia**: Room (SQLite)
- **Arquitectura**: MVVM + Repository
- **Async**: Kotlin Coroutines + Flow
- **Navegación**: Navigation Compose

## Decisiones Clave de Diseño

### 1. Representación del Orden (Fractional Indexing)

Usamos `orderIndex: Double` en lugar de `Int` o reindexado masivo.

**Ventajas:**
- Insertar entre dos ítems sin modificar los demás: si tenemos 1.0 y 2.0, insertamos en 1.5
- Reordenamiento es solo cambiar un número, no actualizar toda la lista
- Múltiples inserciones entre los mismos ítems: 1.0, 1.5, 1.25, 1.375...

**Cuándo reindexamos:**
Cuando el espacio entre índices es menor a 0.0001, hacemos reindexado masivo de esa rama.

### 2. Focus y Navegación

El "focus" se implementa mediante `currentNodeId` en el ViewModel:
- `null` = estamos en la raíz (mostramos nodos sin padre)
- `Long` = estamos enfocados en ese nodo (mostramos solo sus hijos)

El breadcrumb se construye dinámicamente consultando los ancestros del nodo actual.

### 3. Búsqueda con Contexto

Los resultados de búsqueda no muestran solo el ítem encontrado, sino todo su path:
```
Root > Proyecto > Tarea específica
```

Esto da contexto de dónde se encuentra cada resultado en la jerarquía.

### 4. Hashtags y Links

Ambos se detectan mediante regex en el texto y se almacenan como texto plano:
- Hashtag: `#palabra` (alfanumérico y guiones bajos)
- Link interno: `[[contenido]]`

No hay tabla normalizada de tags para el MVP - se computan al vuelo.

### 5. Baja Fricción en Móvil

Mapeo de acciones de teclado (WorkFlowy) a gestos móviles:

| Desktop | Móvil |
|---------|-------|
| Enter (nuevo hermano) | FAB + o IME Action en teclado |
| Tab (indent) | Swipe derecha |
| Shift+Tab (outdent) | Swipe izquierda |
| Flechas arriba/abajo | Menú contextual |
| Click para editar | Tap en el texto |
| Click en bullet para focus | Tap en chevron/ícono |

## Estructura del Proyecto

```
app/src/main/java/com/ma/app/
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt      # Database Room
│   │   ├── Converters.kt       # TypeConverters (Date)
│   │   └── NodeDao.kt          # Data Access Object
│   ├── model/
│   │   └── Node.kt             # Entidades de datos
│   └── repository/
│       └── NodeRepository.kt   # Lógica de negocio
├── ui/
│   ├── components/             # Componentes reutilizables
│   │   ├── Breadcrumb.kt
│   │   └── OutlineItem.kt
│   ├── navigation/
│   │   └── Navigation.kt       # Grafo de navegación
│   ├── screens/                # Pantallas principales
│   │   ├── OutlineScreen.kt
│   │   ├── SearchScreen.kt
│   │   ├── NodeDetailScreen.kt
│   │   └── SettingsScreen.kt
│   └── theme/                  # Tema Material 3
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── utils/
│   ├── TextParser.kt           # Parser de hashtags/links
│   └── MarkdownExporter.kt     # Export/import Markdown
├── viewmodel/                  # ViewModels
│   ├── OutlineViewModel.kt
│   ├── SearchViewModel.kt
│   ├── NodeDetailViewModel.kt
│   └── ExportViewModel.kt
├── MainActivity.kt
└── MaApplication.kt
```

## Cómo Ejecutar

1. Abre el proyecto en Android Studio (versión Hedgehog o posterior recomendada)
2. Sincroniza el proyecto con Gradle (`Sync Project with Gradle Files`)
3. Ejecuta en un emulador o dispositivo físico (min SDK 26)

### Requisitos

- Android Studio Hedgehog (2023.1.1) o posterior
- JDK 17
- Android SDK 34
- Dispositivo/Emulador con Android 8.0+ (API 26)

## Tests

Los tests unitarios cubren:

1. **TextParserTest**: Parser de hashtags y links internos
2. **TreeOperationsTest**: Lógica de árbol y fractional indexing

Para ejecutar tests:
```bash
./gradlew test
```

## Uso

### Crear ítems
- Toca el FAB (+) para agregar un ítem al final
- En edición, el IME action "Next" crea un nuevo ítem hermano

### Organizar
- **Swipe derecha**: Indentar (convertir en hijo del ítem anterior)
- **Swipe izquierda**: Desindentar (convertir en hermano del padre)
- **Menú (⋮)**: Mover arriba/abajo, eliminar

### Navegar
- **Tap en chevron (>)**: Focus en esa rama
- **Breadcrumb**: Toca cualquier ancestro para ir ahí
- **Flecha atrás**: Vuelve al nivel anterior

### Buscar
- Toca el ícono de búsqueda
- Escribe para buscar en títulos y notas
- Los resultados muestran el contexto jerárquico
- Toca un resultado para ir a esa rama

### Hashtags y Links
- Escribe `#tag` en cualquier texto
- Toca el hashtag para buscar todos los ítems con ese tag
- Escribe `[[Nombre de Nodo]]` para crear un link interno

### Exportar
- Ve a Configuración (⋮ > Configuración)
- "Exportar a Markdown" guarda todo el árbol
- "Crear backup" para una copia de seguridad

## Roadmap Futuro

- [ ] Drag & drop para reordenar
- [ ] Atajos de teclado para tablets
- [ ] Tema oscuro automático
- [ ] Widget de inicio
- [ ] Búsqueda por fecha
- [ ] Favoritos / bookmarks
- [ ] Plantillas de nodos

## Licencia

Proyecto personal. Uso exclusivo del desarrollador.

---

**間 (ma)** - El espacio entre las cosas. La pause que da sentido.
