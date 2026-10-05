# KlonMatik V2

Bu sürüm Android üzerinde sanal uygulama alanı kullanan bir klon yöneticisi olarak derlenir.

## Amaç
- Telefonda zaten yüklü kullanıcı uygulamalarını listeden seçmek
- Play Store'dan tekrar indirmeden sanal alana eklemek
- Klonun uygulama verisini ana uygulamadan ayrı tutmak
- Reklamsız kullanmak

## Teknik temel
Apache-2.0 lisanslı Black00Z/Blacks-BlackBox projesinin 40282a7bf4500948cfd598fc67e6e63114b26dd9 commit'i üzerine derlenir.
KlonMatik derlemesinde ana menüde konum taklidi ve cihaz kimliği taklidi seçenekleri gizlenir; kullanım odağı uygulama klonlamadır.

## Sınırlamalar
Sanal uygulama yaklaşımı her uygulamayla yüzde 100 uyumlu değildir. Play Integrity, donanım destekli doğrulama, DRM veya sanallaştırma tespiti kullanan uygulamalar çalışmayabilir.
