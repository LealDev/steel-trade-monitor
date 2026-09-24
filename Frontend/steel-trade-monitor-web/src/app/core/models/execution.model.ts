/** Espelho do DTO ExecutionResponse do backend. */
export interface Execution {
  id: number;
  fonte: string;
  status: 'EM_ANDAMENTO' | 'SUCESSO' | 'FALHA' | 'PARCIAL';
  iniciadoEm: string;
  finalizadoEm: string | null;
  periodoReferencia: string | null;
  registrosGravados: number | null;
  mensagem: string | null;
}

/** Espelho do envelope PageResponse do backend. */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
