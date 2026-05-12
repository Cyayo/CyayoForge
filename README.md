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

### 2. Cara Mengurai (Salvage) Item
- Gunakan `/cf salvage`.
- Masukkan item yang ingin diurai. Kamu akan mendapatkan sebagian material kembali.
- Fitur ini sangat berguna untuk mendaur ulang peralatan lama.

### 3. Cara Memberikan Item Lewat Command (Admin)
- `/cf give <player> <item_id> [tier] [quality]`
- Contoh: `/cf give Cyayo SUPER_SWORD EPIC PERFECT`

```text
=========================================
      CYAYOFORGE - COMMANDS & PERMS
=========================================

USER COMMANDS:
/cf open [type]              > Buka GUI Forge (WEAPON/ARMOR).
/cf salvage                  > Memasuki menu urai item (Salvage).
/cf inspect                  > Cek material penyusun item yang dipegang.

ADMIN COMMANDS:
/cf open <player> [type]     > Buka GUI forge untuk pemain tertentu.
/cf salvage <player>         > Buka menu salvage untuk pemain lain.
/cf give <player> <id> [t][q]> Beri item tempa spesifik ke pemain.
/cf reset <player|all>       > Reset statistik penempaan pemain.
/cf unstuck <player>         > Berhentikan sesi forge player yang macet.
/cf reload                   > Reload konfigurasi plugin.
/cf version                  > Cek versi plugin.

PERMISSIONS:
cyayoforge.use               > Izin dasar untuk menempa.
cyayoforge.salvage           > Izin untuk mengurai item.
cyayoforge.inspect           > Izin untuk melihat material item.
cyayoforge.open              > Izin buka menu untuk orang lain.
cyayoforge.give              > Izin create item lewat command.
cyayoforge.reset             > Izin reset data pemain.
cyayoforge.reload            > Izin reload plugin.
cyayoforge.admin.unstuck     > Izin darurat hentikan sesi forge.
cyayoforge.bonus.<name>      > Bonus poin minigame (Setting di config).
```

```text
=========================================
             PLACEHOLDERS
=========================================

%cyayoforge_total_forged%        > Total item yang pernah ditempa.
%cyayoforge_total_forged_weapon% > Jumlah senjata yang diciptakan.
%cyayoforge_total_forged_armor%  > Jumlah armor yang diciptakan.
%cyayoforge_last_forged%         > Nama item terakhir yang ditempa.
%cyayoforge_penalty_time%        > Sisa waktu hukuman forge.
```

### 🚀 Plugin Features
- 🎮 **Interactive Minigame** - Penempaan bukan sekedar klik, tapi butuh skill!
- ♻️ **Deep Salvage System** - Sistem urai item yang adil dan dinamis.
- 💎 **MMOItems Integrated** - Mendukung penuh kustomisasi item MMOItems.
- 📈 **Dynamic Quality** - Hasil kualitas item ditentukan oleh performa player.
- 🎁 **Tiered Forging** - Support sistem Tier (Common, Rare, Epic, Legendary).
