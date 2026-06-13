# Axos Core — README de Arquitectura

> Versión objetivo: Paper 1.21.1 | Java 21 | Maven

---

## Estructura de Paquetes — `src/main/java/cl/pandress/`

```
cl.pandress/
├── Axos.java                        # Main class (JavaPlugin)
│
├── command/
│   ├── player/                      # Comandos ejecutables por jugadores (/spawn, /ec, etc.)
│   └── admin/                       # Comandos solo para admins (/setspawn, /gamemode, etc.)
│
├── db/
│   └── mysql/                       # Conexión y queries MySQL (opcional, togglable)
│
├── modules/                         # Cada módulo que se vaya creando vive aquí
│   └── (nombre_modulo)/
│
├── essentials/                      # Lógica de comandos básicos del servidor
│   └── (nombre_feature)/            # Ej: spawn/, ec/, gamemode/
│
└── utils/                           # Utilidades globales reutilizables
    ├── ChatUtil.java                 # Colores, Components, formato de texto
    ├── YamlFile.java                 # Wrapper para archivos YAML
    ├── WebhookUtil.java              # Envío de webhooks Discord (async)
    └── (otros utils según se necesite)
```

---

## Estructura de Recursos — `src/main/resources/`

```
resources/
├── plugin.yml
├── config.yml                       # Config principal del plugin (settings globales)
├── database.yml                     # Config de MySQL (host, port, user, pass, db, enabled: true/false)
│
└── modules/
    └── (nombre_modulo)/             # Cada módulo tiene su carpeta con su config y messages
        ├── config.yml
        └── messages.yml
```

---

## Reglas de Arquitectura

| Regla | Detalle |
|---|---|
| Comandos | Van en `command/player/` o `command/admin/` según quién los usa |
| Lógica | Va en `essentials/(feature)/` — los comandos solo llaman al manager |
| Módulos | Todo módulo nuevo va en `modules/(nombre)/` (java) y `resources/modules/(nombre)/` (yaml) |
| Base de datos | `db/mysql/` — activable/desactivable desde `database.yml` con `enabled: true/false` |
| Utils | Solo utilidades globales sin lógica de negocio |

---

## Convenciones de Nombrado

- Managers: `NombreManager.java` → contiene la lógica
- Listeners: `NombreListener.java` → solo eventos
- Commands: `NombreCommand.java` → solo parsing de args + llamada al manager
- Config keys en `kebab-case`
- Permisos: `axos.(admin|player).(feature)`

---

## Módulos Registrados

> Se irán agregando aquí a medida que se creen.

| Módulo | Descripción | Estado |
|---|---|---|
| essentials/spawn | /spawn, /setspawn, death-spawn | ✅ Existente |
| essentials/gamemode | /gamemode (/gm) | ✅ Existente |

---

## Notas Pendientes

- [ ] Arreglar detección de movimiento en `SpawnCommand` (yaw/pitch + umbral)
- [ ] Configurar Maven filtering para `${project.version}` en `plugin.yml`
- [ ] Eliminar doble teleport en `DeathSpawnListener`
- [ ] Sistema de `/axos reload` por módulos
- [ ] Implementar `db/mysql/` con toggle desde `database.yml`
