---
name: sync-upstream-bakasu
description: Hướng dẫn agent sync code từ upstream BakaSU về fork AweYukiSU an toàn, không làm mất commit local khi xảy ra conflict.
---

# Sync Upstream BakaSU (An toàn, không mất commit local)

Skill này hướng dẫn quy trình đồng bộ (sync) thay đổi mới từ repository gốc (**BakaSU**) về fork **AweYukiSU** (`com.hzzmonnet.aweyuki`) mà vẫn bảo toàn toàn bộ commit và tùy chỉnh riêng của dự án.

## Nguyên tắc cốt lõi

1. **Không bao giờ** dùng `git reset --hard upstream/...` hoặc `git checkout --theirs` bừa bãi làm mất commit local.
2. **Luôn tạo nhánh backup** trước khi merge/rebase để có thể khôi phục tức thì nếu xử lý conflict nhầm.
3. **Ưu tiên `git merge`** (hoặc `git rebase` trên nhánh phụ) để giữ lịch sử rõ ràng và xử lý conflict từng file một cách có kiểm soát.
4. **Giữ vững các tùy chỉnh đặc thù của AweYukiSU**:
   - Tên dự án: `AweYukiSU` (không để lọt `BakaSU` / `bakasu` mới từ upstream).
   - Package name: `com.hzzmonnet.aweyuki` (thay cho `org.bakasu.bakasu` của upstream).
   - Không hỗ trợ multi-manager (`CONFIG_KSU_MULTI_MANAGER_SUPPORT` đã bị loại bỏ khỏi `kernel/`).

---

## Quy trình thực hiện

### Bước 1: Kiểm tra trạng thái & tạo nhánh backup
Đảm bảo working tree sạch (commit hoặc stash thay đổi chưa lưu), sau đó tạo nhánh backup từ commit hiện tại:

```bash
git status -s
git branch backup/pre-sync-$(date +%Y%m%d-%H%M%S)
```

### Bước 2: Fetch upstream BakaSU
Kiểm tra remote `upstream` (nếu chưa có thì thêm `git remote add upstream <url-repo-bakasu>`):

```bash
git remote -v
git fetch upstream
```

### Bước 3: Xem trước các commit mới từ upstream
Kiểm tra những gì sắp được gộp vào để lường trước vùng dễ conflict:

```bash
git log --oneline HEAD..upstream/main
```
*(Thay `main` bằng nhánh chính thực tế của upstream nếu khác.)*

### Bước 4: Merge upstream vào nhánh hiện tại
Thực hiện merge (không dùng `--squash` nếu muốn giữ lịch sử, hoặc dùng `--no-ff` để đánh dấu mốc sync):

```bash
git merge upstream/main --no-commit --no-ff
```

Nếu không có conflict, kiểm tra lại diff và commit. Nếu có conflict, chuyển sang **Bước 5**.

### Bước 5: Xử lý Conflict đúng cách (Không mất commit local)

Liệt kê các file bị conflict:
```bash
git diff --name-only --diff-filter=U
```

Với từng file conflict:
1. **Đọc kỹ cả hai phía** (`<<<<<<< HEAD` là code của AweYukiSU, `>>>>>>> upstream/main` là code mới từ BakaSU).
2. **File đã đổi tên/di chuyển package**:
   - Upstream sửa trong `org/bakasu/bakasu/...`, trong khi repo này nằm ở `com/hzzmonnet/aweyuki/...`.
   - Nếu Git báo `modify/delete` hoặc `rename/delete` conflict: **KHÔNG** xóa file ở `com/hzzmonnet/aweyukisu` hay `com/hzzmonnet/aweyuki`. Hãy lấy phần thay đổi logic từ upstream áp dụng vào file tương ứng trong `com/hzzmonnet/aweyuki/`, sau đó `git rm` file phía `org/bakasu/...` nếu nó bị tạo thừa.
3. **Thay thế định danh từ upstream**:
   - Mọi code mới lấy từ upstream có chứa `org.bakasu.bakasu` → đổi thành `com.hzzmonnet.aweyuki`.
   - Mọi chuỗi/macro `BakaSU` / `bakasu` / `BAKASU` → đổi thành `AweYukiSU` / `aweyukisu` / `AWEYUKISU`.
4. **Multi-manager**:
   - Nếu upstream thêm code liên quan `CONFIG_KSU_MULTI_MANAGER_SUPPORT` trong `kernel/Kconfig`, `kernel/Kbuild`, `kernel/build-all.sh`, `kernel/tools/ddk_compatible.mk`, `kernel/manager/apk_sign.c` → bỏ qua hoặc loại bỏ phần multi-manager đó để giữ đúng cấu hình của AweYukiSU.

Sau khi sửa xong từng file:
```bash
git add <file-đã-xử-lý>
```

### Bước 6: Kiểm tra lại & Commit
1. Kiểm tra không còn marker conflict (`<<<<<<<`, `=======`, `>>>>>>>`):
   ```bash
   rg -n "^(<<<<<<<|=======|>>>>>>>)"
   ```
2. Kiểm tra không còn sót `org.bakasu.bakasu` hoặc `CONFIG_KSU_MULTI_MANAGER_SUPPORT`:
   ```bash
   rg -n "org\.bakasu\.bakasu|CONFIG_KSU_MULTI_MANAGER_SUPPORT"
   ```
3. Hoàn tất merge commit:
   ```bash
   git commit -m "chore: sync upstream BakaSU"
   ```

### Khôi phục nếu có sự cố
Nếu quá trình merge bị rối và muốn quay lại trạng thái ban đầu:
```bash
git merge --abort
# Hoặc nếu đã commit nhầm:
git reset --hard backup/pre-sync-<timestamp>
```
