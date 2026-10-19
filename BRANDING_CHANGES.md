# aMACEzing Sales branding update

This update uses the supplied aMACEzing Printing Services logo for the Android launcher icon and in-app branding.

## Changed files
- `app/src/main/res/drawable/amacezing_logo.jpg` — replaced with the supplied logo.
- `app/src/main/res/drawable/ic_launcher_foreground_img.png` — adaptive launcher icon foreground uses the supplied logo.
- `app/src/main/res/mipmap-*/ic_launcher*.png` — regenerated density-specific launcher icons.
- `app/src/main/java/com/example/MainActivity.kt` — top app bar now displays the logo instead of the text-only wordmark.
- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt` — dashboard header includes the logo and uses the `aMACEzing Sales` casing.
- `app/src/main/java/com/example/ui/theme/Color.kt` — accent palette changed from teal/orange to electric blue/cyan to match the supplied logo.

The business/navigation icons and their click handlers remain in place; only shared brand colors and header branding were changed.

## Build
Commit these changes to GitHub and run the existing Android build workflow. A local build could not be verified in this environment because Gradle's distribution could not be downloaded (network unavailable).
