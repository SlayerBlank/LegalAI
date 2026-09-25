package pe.edu.upc.legalai.DTOs.request;

public class DraftsRequestDTO {
    public Long getExpediente_id() {
        return expediente_id;
    }

    public void setExpediente_id(Long expediente_id) {
        this.expediente_id = expediente_id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getUsuario_id() {
        return usuario_id;
    }

    public void setUsuario_id(Long usuario_id) {
        this.usuario_id = usuario_id;
    }

    private Long expediente_id;
    private Long usuario_id;
    private String title;
    private String prompt;
    private String content;
    private String status;

}