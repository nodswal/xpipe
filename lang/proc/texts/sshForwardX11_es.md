## Reenvío X11

Cuando esta opción está activada, la conexión SSH se iniciará con el reenvío X11 configurado. En Linux, esto suele funcionar de forma inmediata y no requiere ninguna configuración. En macOS, necesitas que se ejecute un servidor X11 como [XQuartz](https://www.xquartz.org/) en tu máquina local.

### X11 en Windows

XPipe te permite utilizar las capacidades X11 de WSL2 para tu conexión SSH. Lo único que necesitas para ello es una distribución [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) instalada en tu sistema local. XPipe elegirá automáticamente una distribución instalada compatible si es posible, pero también puedes utilizar otra en el menú de configuración.

Esto significa que no necesitas instalar un servidor X11 independiente en Windows. Sin embargo, si estás utilizando uno de todos modos, XPipe lo detectará y utilizará el servidor X11 que esté ejecutándose en ese momento.
