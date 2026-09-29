package pe.edu.upc.legalai.DTOs.response;

public class IAResponseDTO {
    private String answer;
    private String provider;
    private String model;

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
