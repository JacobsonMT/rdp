/*
 * The rdp project
 * 
 * Copyright (c) 2014 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubc.pavlab.rdp.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.TaxonRepository;
import ubc.pavlab.rdp.repositories.UserGeneRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("userGeneService")
public class UserGeneServiceImpl implements UserGeneService {

    @Autowired
    private TaxonRepository taxonRepository;

    @Autowired
    private UserGeneRepository userGeneRepository;

    @Override
    public Integer countUniqueAssociations() {
        return userGeneRepository.countDistinctGeneByTierIn( TierType.MANUAL_TIERS );
    }

    @Override
    public Integer countAssociations() {
        return userGeneRepository.countByTierIn( TierType.MANUAL_TIERS );
    }

    @Override
    public Map<String, Integer> researcherCountByTaxon() {
        Map<String, Integer> countByTaxon = new HashMap<>();
        for ( Taxon taxon : taxonRepository.findByActiveTrue() ) {
            countByTaxon.put(taxon.getCommonName(), userGeneRepository.countDistinctUserByGeneTaxon( taxon ));
        }

        return countByTaxon;
    }

}
