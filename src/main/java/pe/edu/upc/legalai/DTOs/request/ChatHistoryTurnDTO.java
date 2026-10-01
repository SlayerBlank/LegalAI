package pe.edu.upc.legalai.DTOs.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Turno previo de la conversacion usado solo para reformular la consulta de
 * recuperacion y para dar contexto al modelo. Nunca se persiste ni se expone.
 */
@Schema(description = "Turno previo USER/ASSISTANT del historial autorizado de la misma sesion")
public record ChatHistoryTurnDTO(String role, String content) { }