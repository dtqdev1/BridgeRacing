package me.dtqdev.bridgeracing.data;

/**
 * Đại diện cho một bậc Rank, được tải từ ranks.yml.
 */
public class EloRank {
    private final String id; // Ví dụ: "GOLD", "DIAMOND"
    private final int fromElo; // ELO bắt đầu
    private final int toElo;   // ELO kết thúc
    private final String displayName; // Tên hiển thị có màu, ví dụ: "&6Gold"

    public EloRank(String id, int fromElo, int toElo, String displayName) {
        this.id = id;
        this.fromElo = fromElo;
        this.toElo = toElo;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public int getFromElo() {
        return fromElo;
    }

    public int getToElo() {
        return toElo;
    }

    public String getDisplayName() {
        return displayName;
    }
}