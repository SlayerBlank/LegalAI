package pe.edu.upc.legalai.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_messages")
public class Mensajes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    //@ManyToOne(fetch = FetchType.LAZY, optional = false)
    //@JoinColumn(name = "session_id", nullable = false)
    //private SesionChat sesion;

    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "mensaje")
    private List<CitacionesIA> citaciones = new ArrayList<>();

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    //public SesionChat getSesion() {return sesion;}

    //public void setSesion(SesionChat sesion) {this.sesion = sesion;}

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<CitacionesIA> getCitaciones() {
        return citaciones;
    }

    public void setCitaciones(List<CitacionesIA> citaciones) {
        this.citaciones = citaciones;
    }
}
