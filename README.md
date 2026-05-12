# ⚒ CyayoForge - The Ultimate Forging System

"Tempa takdirmu, ukir namamu dalam sejarah legendaris. Sistem penempaan paling interaktif dengan integrasi MMOItems."

```text
=========================================
             QUICK TUTORIAL
=========================================
```

### 1. Cara Menempa Item
- Gunakan command `/cf open WEAPON` atau `/cf open ARMOR`.
- Masukkan material yang dibutuhkan (biasanya dari MMOItems).
- Mainkan **Minigame** yang muncul. Semakin tinggi skor minigame, semakin bagus kualitas item yang dihasilkan.
- Hasil kualitas ditentukan oleh **Quality Threshold** (Perfect, Exquisite, Good, Flawed, Broken).

### 2. Cara Mengurai (Salvage) Item
- Gunakan `/cf salvage`. Masukkan item untuk diurai kembali menjadi material.
- Waktu penguraian bergantung pada **Tier** (Poor, Great, Relic, dll) dan **Quality** item tersebut.

### 3. Bonus Permission (Customizable)
Kamu bisa memberikan bonus skor minigame untuk Rank/VIP tertentu di `config.yml`:
```yaml
permission_bonuses:
  cyayoforge.bonus.vip: 10.0   # +10% Skor Minigame
  cyayoforge.bonus.mvp: 25.0   # +25% Skor Minigame
```

```text
=========================================
      CYAYOFORGE - COMMANDS & PERMS
=========================================

USER COMMANDS:
/cf open [type]              > Buka GUI Forge (WEAPON/ARMOR).
/cf salvage                  > Menu urai item (Salvage).
/cf inspect                  > Cek material penyusun item.

ADMIN COMMANDS:
/cf open <player> [type]     > Buka GUI forge untuk pemain lain.
/cf salvage <player>         > Buka menu salvage untuk pemain lain.
/cf give <player> <id> [t][q]> Beri item tempa spesifik.
/cf reset <player|all>       > Reset data penempaan pemain.
/cf unstuck <player>         > Hentikan sesi forge yang macet.
/cf reload                   > Reload konfigurasi plugin.
/cf version                  > Cek versi plugin.

PERMISSIONS:
cyayoforge.use               > Izin dasar menempa.
cyayoforge.salvage           > Izin mengurai item.
cyayoforge.inspect           > Izin cek material item.
cyayoforge.open              > Izin buka menu untuk orang lain.
cyayoforge.give              > Izin create item lewat command.
cyayoforge.reset             > Izin reset data pemain.
cyayoforge.reload            > Izin reload plugin.
cyayoforge.admin.unstuck     > Izin darurat hentikan sesi forge.
cyayoforge.admin.salvage     > Izin buka salvage player lain.
cyayoforge.setquality        > Izin debug set kualitas item.
cyayoforge.bypass.animation  > Lewati animasi tempa.
cyayoforge.bypass.minigame   > Lewati minigame tempa.
cyayoforge.admin             > Master permission (Akses Semua).

[!] CUSTOM BONUS PERMISSION:
Kamu bisa membuat permission baru untuk bonus skor minigame.

CARA MENGATUR:
Buka 'config.yml' pada bagian 'minigame.permission_bonuses'. 
Tambahkan node permission dan nilai bonus persentasenya.
Contoh: 'cyayoforge.bonus.vip: 15.0'

FUNGSINYA:
Memberikan keuntungan bagi player dengan rank tertentu agar lebih 
mudah mendapatkan item kualitas tinggi melalui bonus skor minigame.
```

```text
=========================================
             PLACEHOLDERS
=========================================

%cyayoforge_total_forged%        > Total item ditempa.
%cyayoforge_total_forged_weapon% > Total senjata diciptakan.
%cyayoforge_total_forged_armor%  > Total armor diciptakan.
%cyayoforge_last_forged%         > Item terakhir yang ditempa.
%cyayoforge_penalty_time%        > Sisa waktu hukuman forge.
```

### 🚀 Advanced Features
- 🎮 **Skill-Based Minigame** - Keberhasilan tempaan ditentukan oleh ketepatanmu saat minigame, bukan sekedar RNG.
- 🎬 **Immersive Animations** - Efek Title dan Subtitle disertai suara menempa yang realistis (Anvil, Fire, Extinguish).
- 🏷️ **Stat Injection** - Otomatis menyuntikkan data **Nama Penempa** dan **Kualitas** ke dalam lore dan stat MMOItems.
- ♻️ **Advanced Salvage** - Penguraian item yang adil dengan sistem waktu yang dipengaruhi oleh tingkat kelangkaan item.
- 📈 **Custom Tiers** - Mendukung berbagai tingkatan tier dari MMOItems (Relic, Ancient, Secret, dll).
- 🔊 **Feedback Audio** - Suara berbeda untuk setiap hasil tempaan (Perfect Hit, Good Hit, Broken, Miss).
- 🛡️ **Anti-Abuse Penalty** - Player yang sengaja menutup menu saat minigame akan terkena penalti cooldown.
