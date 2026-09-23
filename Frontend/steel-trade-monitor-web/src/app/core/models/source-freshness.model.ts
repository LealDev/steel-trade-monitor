/** Espelho do DTO SourceFreshnessResponse do backend. */
export interface SourceFreshness {
  fonte: string;
  status: 'EM_ANDAMENTO' | 'SUCESSO' | 'FALHA' | 'PARCIAL';
  iniciadoEm: string;
  finalizadoEm: string | null;
  periodoReferencia: string | null;
  registrosGravados: number | null;
  mensagem: string | null;
  ultimoSucessoEm: string | null;
}
