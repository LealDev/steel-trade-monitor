package com.steeltrade.warehouse.loader;

import com.steeltrade.ingestion.comexstat.dto.RegistroComercioExterior;
import com.steeltrade.warehouse.dimension.Country;
import com.steeltrade.warehouse.dimension.FederativeUnit;
import com.steeltrade.warehouse.dimension.Ncm;
import com.steeltrade.warehouse.dimension.TimeDimension;
import com.steeltrade.warehouse.dimension.TransportMode;
import com.steeltrade.warehouse.dimension.repository.CountryRepository;
import com.steeltrade.warehouse.dimension.repository.FederativeUnitRepository;
import com.steeltrade.warehouse.dimension.repository.NcmRepository;
import com.steeltrade.warehouse.dimension.repository.TimeDimensionRepository;
import com.steeltrade.warehouse.dimension.repository.TransportModeRepository;

import org.springframework.stereotype.Service;

@Service
public class DimensionSyncServiceImpl implements DimensionSyncService {

    private final TimeDimensionRepository tempoRepository;
    private final NcmRepository ncmRepository;
    private final CountryRepository paisRepository;
    private final FederativeUnitRepository ufRepository;
    private final TransportModeRepository viaRepository;

    public DimensionSyncServiceImpl(TimeDimensionRepository tempoRepository,
                                    NcmRepository ncmRepository,
                                    CountryRepository paisRepository,
                                    FederativeUnitRepository ufRepository,
                                    TransportModeRepository viaRepository) {
        this.tempoRepository = tempoRepository;
        this.ncmRepository = ncmRepository;
        this.paisRepository = paisRepository;
        this.ufRepository = ufRepository;
        this.viaRepository = viaRepository;
    }

    @Override
    public DimensionKeys sincronizar(RegistroComercioExterior registro) {
        var tempo = tempoRepository.findByAnoAndMes(registro.ano(), registro.mes())
                .orElseGet(() -> tempoRepository.save(TimeDimension.de(registro.ano(), registro.mes())));

        var ncm = ncmRepository.findByCodigoNcm(registro.codigoNcm())
                .orElseGet(() -> ncmRepository.save(new Ncm(registro.codigoNcm(), registro.descricaoNcm(),
                        registro.sh4(), registro.sh6(), registro.capitulo())));

        var pais = paisRepository.findByNomePt(registro.nomePais())
                .orElseGet(() -> paisRepository.save(new Country(registro.nomePais())));

        var uf = ufRepository.findByNome(registro.nomeUf())
                .orElseGet(() -> ufRepository.save(new FederativeUnit(registro.nomeUf())));

        var via = viaRepository.findByDescricao(registro.descricaoVia())
                .orElseGet(() -> viaRepository.save(new TransportMode(registro.descricaoVia())));

        return new DimensionKeys(tempo.getId(), ncm.getId(), pais.getId(), uf.getId(), via.getId());
    }
}
