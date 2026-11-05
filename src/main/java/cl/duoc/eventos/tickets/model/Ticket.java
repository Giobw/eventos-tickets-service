package cl.duoc.eventos.tickets.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String code;

    @Column(nullable = false, length = 50)
    private String eventId;

    @Column(nullable = false, length = 120)
    private String eventName;

    @Column(nullable = false, length = 50)
    private String userId;

    @Column(nullable = false, length = 120)
    private String userName;

    @Column(length = 20)
    private String seat;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    /** Nombre del archivo PDF, por ejemplo ticket00001.pdf */
    @Column(length = 100)
    private String fileName;

    /** Ruta relativa dentro del EFS: {eventId}/{userId}/{fileName} */
    @Column(length = 300)
    private String efsPath;

    /** Clave del objeto en S3 (misma estructura que la ruta del EFS). */
    @Column(length = 300)
    private String s3Key;

    @Column(nullable = false)
    private boolean uploadedToS3;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getSeat() { return seat; }
    public void setSeat(String seat) { this.seat = seat; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getEfsPath() { return efsPath; }
    public void setEfsPath(String efsPath) { this.efsPath = efsPath; }
    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public boolean isUploadedToS3() { return uploadedToS3; }
    public void setUploadedToS3(boolean uploadedToS3) { this.uploadedToS3 = uploadedToS3; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
