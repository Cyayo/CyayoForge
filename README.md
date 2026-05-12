# ⚒ CyayoForge - The Ultimate Forging System
> "Tempa takdirmu, ukir namamu dalam sejarah legendaris."

**CyayoForge** adalah plugin penempaan tingkat lanjut yang terintegrasi penuh dengan **MMOItems**. Hadir dengan sistem minigame interaktif, kualitas tempaan dinamis, dan fitur *salvage* (urai item) yang mendalam untuk memberikan pengalaman crafting yang belum pernah ada sebelumnya.

---

## ╔══════════════════════════════════════════════╗
## ║      CYAYOFORGE - COMMANDS & PERMS         ║
## ╚══════════════════════════════════════════════╝

### ⚔️ USER COMMANDS
Perintah yang dapat digunakan oleh seluruh pemain:

*   **/cf open [type]**                        
    > Membuka GUI penempaan utama. Pilih antara `WEAPON` atau `ARMOR`.
*   **/cf salvage**                            
    > Memasuki menu urai item. Kembalikan material berhargamu dari peralatan lama.
*   **/cf inspect**                            
    > Ingin tahu rahasia di balik pedangmu? Cek material penyusun item yang sedang dipegang.

### 🛡️ ADMIN COMMANDS
Perintah kendali penuh untuk para penguasa Forge:

*   **/cf open <player> [type]**               
    > Memaksa membuka GUI forge untuk pemain tertentu.
*   **/cf salvage <player>**                   
    > Membuka menu salvage secara paksa untuk pemain lain.
*   **/cf give <player> <item> [tier] [qual]** 
    > Menciptakan item tempa spesifik langsung ke inventory pemain.
*   **/cf reset <player|all>**                 
    > Menghapus sejarah dan statistik statistik penempaan pemain.
*   **/cf unstuck <player>**                   
    > Solusi darurat untuk menghentikan sesi forge pemain yang terhenti.
*   **/cf reload**                             
    > Memperbarui semua konfigurasi tanpa perlu restart server.
*   **/cf version**                            
    > Menampilkan identitas dan versi sang maha karya CyayoForge.

---

## 🔑 PERMISSIONS
Berikut adalah daftar kunci akses untuk mengontrol kekuatan di servermu:

### 📜 Basic Permissions
| Node | Deskripsi |
| :--- | :--- |
| `cyayoforge.use` | Izin dasar untuk menempa peralatan. |
| `cyayoforge.salvage` | Izin untuk mengurai item kembali ke material asal. |
| `cyayoforge.inspect` | Izin untuk melihat detail material pembuat item. |

### 🛠️ Admin Permissions
| Node | Deskripsi |
| :--- | :--- |
| `cyayoforge.open` | Izin membuka menu forge untuk orang lain. |
| `cyayoforge.give` | Izin menciptakan item legendaris lewat perintah. |
| `cyayoforge.reset` | Izin melakukan reset data pemain. |
| `cyayoforge.reload` | Izin melakukan konfigurasi ulang secara realtime. |
| `cyayoforge.admin.unstuck` | Izin darurat untuk membebaskan sesi forge. |

### 💎 CUSTOMIZABLE PERMISSIONS (Dapat Diatur!)
Permission ini dapat kamu tambah, kurangi, atau ubah nilainya di `config.yml` bagian `minigame.permission_bonuses`. Gunakan ini untuk memberikan keuntungan bagi Rank atau VIP tertentu:

*   `cyayoforge.bonus.apprentice`  > **+5.0%** Bonus Poin Minigame.
*   `cyayoforge.bonus.master`      > **+15.0%** Bonus Poin Minigame.
*   `cyayoforge.bonus.grandmaster` > **+30.0%** Bonus Poin Minigame.
*   *(Kamu bisa membuat node baru sesuai selera di config!)*

---

## 📊 PLACEHOLDERS
Integrasi PlaceholderAPI untuk menampilkan statistik heroik pemain:

*   `%cyayoforge_total_forged%`        > Total seluruh item yang pernah ditempa.
*   `%cyayoforge_total_forged_weapon%` > Jumlah senjata yang berhasil diciptakan.
*   `%cyayoforge_total_forged_armor%`  > Jumlah armor yang berhasil diciptakan.
*   `%cyayoforge_last_forged%`         > Nama item terakhir yang keluar dari bara api.
*   `%cyayoforge_penalty_time%`        > Sisa waktu hukuman bagi penempa yang curang.

---
*Created with ❤️ by **Cyayo** for the best RPG experience.*
