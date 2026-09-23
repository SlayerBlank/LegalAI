package pe.edu.upc.legalai.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.edu.upc.legalai.entities.Cliente;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.DTOs.request.ClienteRequestDTO;
import pe.edu.upc.legalai.DTOs.request.DocumentoRequestDTO;
import pe.edu.upc.legalai.DTOs.request.ExpedienteRequestDTO;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        // Solo campos simples autorizados; nuevos campos no se incorporan automaticamente.
        mapper.getConfiguration().setImplicitMappingEnabled(false);
        mapper.createTypeMap(ClienteRequestDTO.class, Cliente.class).addMappings(mapping -> {
            mapping.map(ClienteRequestDTO::getClientType, Cliente::setClientType);
            mapping.map(ClienteRequestDTO::getFullNameOrCompany, Cliente::setFullNameOrCompany);
            mapping.map(ClienteRequestDTO::getDocumentNumber, Cliente::setDocumentNumber);
            mapping.map(ClienteRequestDTO::getEmail, Cliente::setEmail);
            mapping.map(ClienteRequestDTO::getPhone, Cliente::setPhone);
            mapping.map(ClienteRequestDTO::getAddress, Cliente::setAddress);
        });
        mapper.createTypeMap(DocumentoRequestDTO.class, Documento.class).addMappings(mapping -> {
            mapping.map(DocumentoRequestDTO::getFileName, Documento::setFileName);
            mapping.map(DocumentoRequestDTO::getFileType, Documento::setFileType);
            mapping.map(DocumentoRequestDTO::getStorageUrl, Documento::setStorageUrl);
            mapping.map(DocumentoRequestDTO::getCategory, Documento::setCategory);
            mapping.map(DocumentoRequestDTO::getSizeBytes, Documento::setSizeBytes);
        });
        mapper.createTypeMap(ExpedienteRequestDTO.class, Expediente.class).addMappings(mapping -> {
            mapping.map(ExpedienteRequestDTO::getTitle, Expediente::setTitle);
            mapping.map(ExpedienteRequestDTO::getDescription, Expediente::setDescription);
        });
        return mapper;
    }
}
