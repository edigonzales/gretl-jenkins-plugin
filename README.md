# GRETL Console

Die GRETL Console zeigt einen bereitgestellten Schema-V2-Katalog, native
Jenkins-Ausführungen und offene Freigaben. Sie verwendet die Rechte des
jeweiligen Jenkins-Jobs. Die GRETL-Pipeline und der Kataloggenerator liegen im
[Jenkins-Labor](https://github.com/edigonzales/gretl-jenkins-lab).

Übernommen aus dem J3-Plugin am Labor-Commit
`7e448670f115e1bc48293411497e67c60fe53b22`.

## Build und Installation

Mit JDK 25 und Maven `mvn -B -ntp verify` ausführen. Die Datei
`target/gretl-console.hpi` in Jenkins installieren und Jenkins neu starten.
Die Console ist unter `/gretl-console/` verfügbar.

Die Jenkins-Baseline ist 2.555.3. Das Plugin verwendet Pipeline: Groovy,
Pipeline: Job und Pipeline: Input Step. GRETL-Kataloge mit Upload-Feldern
benötigen ausserdem das File Parameters Plugin.
