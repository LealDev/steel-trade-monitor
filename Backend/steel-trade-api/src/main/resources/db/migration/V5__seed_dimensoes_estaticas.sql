-- V5: seed das dimensões estáticas (UFs e vias de transporte conhecidas).
-- O nome é a chave natural de lookup: é o que o payload do Comex Stat traz.
-- Valores desconhecidos que chegarem do payload são inseridos dinamicamente pelo loader.

INSERT INTO dim_uf (sigla, nome, regiao) VALUES
    ('AC', 'Acre', 'Norte'),
    ('AL', 'Alagoas', 'Nordeste'),
    ('AP', 'Amapá', 'Norte'),
    ('AM', 'Amazonas', 'Norte'),
    ('BA', 'Bahia', 'Nordeste'),
    ('CE', 'Ceará', 'Nordeste'),
    ('DF', 'Distrito Federal', 'Centro-Oeste'),
    ('ES', 'Espírito Santo', 'Sudeste'),
    ('GO', 'Goiás', 'Centro-Oeste'),
    ('MA', 'Maranhão', 'Nordeste'),
    ('MT', 'Mato Grosso', 'Centro-Oeste'),
    ('MS', 'Mato Grosso do Sul', 'Centro-Oeste'),
    ('MG', 'Minas Gerais', 'Sudeste'),
    ('PA', 'Pará', 'Norte'),
    ('PB', 'Paraíba', 'Nordeste'),
    ('PR', 'Paraná', 'Sul'),
    ('PE', 'Pernambuco', 'Nordeste'),
    ('PI', 'Piauí', 'Nordeste'),
    ('RJ', 'Rio de Janeiro', 'Sudeste'),
    ('RN', 'Rio Grande do Norte', 'Nordeste'),
    ('RS', 'Rio Grande do Sul', 'Sul'),
    ('RO', 'Rondônia', 'Norte'),
    ('RR', 'Roraima', 'Norte'),
    ('SC', 'Santa Catarina', 'Sul'),
    ('SP', 'São Paulo', 'Sudeste'),
    ('SE', 'Sergipe', 'Nordeste'),
    ('TO', 'Tocantins', 'Norte'),
    (NULL, 'Não Declarada', NULL);

INSERT INTO dim_via_transporte (codigo, descricao) VALUES
    ('01', 'MARITIMA'),
    ('02', 'FLUVIAL'),
    ('03', 'LACUSTRE'),
    ('04', 'AEREA'),
    ('05', 'POSTAL'),
    ('06', 'FERROVIARIA'),
    ('07', 'RODOVIARIA'),
    ('08', 'CONDUTO/REDE DE TRANSMISSAO'),
    ('09', 'MEIOS PROPRIOS'),
    ('10', 'ENTRADA/SAIDA FICTA');
