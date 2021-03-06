package ubc.pavlab.rdp.controllers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.GeneService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.services.UserService;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@RestController
public class UserController {

    private static Log log = LogFactory.getLog( UserController.class );

    @Autowired
    private UserService userService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private GOService goService;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class Model {

        private Map<Integer, TierType> geneTierMap;
        private List<String> goIds;
        private String description;

    }

    @RequestMapping(value = "/user/{id}/delete", method = RequestMethod.GET)
    public String deleteUser( @PathVariable int id ) {
        User user = userService.findUserById( id );
        if ( user != null ) {
            userService.delete( user );
        }
        return "Deleted.";
    }

    @RequestMapping(value = "/user/profile", method = RequestMethod.POST)
    public String saveProfile( @RequestBody @Valid Profile profile ) {
        User user = userService.findCurrentUser();
        user.getProfile().setDepartment( profile.getDepartment() );
        user.getProfile().setDescription( profile.getDescription() );
        user.getProfile().setLastName( profile.getLastName() );
        user.getProfile().setName( profile.getName() );
        user.getProfile().setOrganization( profile.getOrganization() );
        user.getProfile().setPhone( profile.getPhone() );
        user.getProfile().setWebsite( profile.getWebsite() );

        userService.updatePublications( user, profile.getPublications() );

        return "Saved.";
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public User getUser() {
        return userService.findCurrentUser();
    }

    @RequestMapping(value = "/user/taxon", method = RequestMethod.GET)
    public Set<Taxon> getTaxons() {
        return userService.findCurrentUser().getUserGenes().values().stream().map( UserGene::getTaxon ).collect( Collectors.toSet() );
    }

    @RequestMapping(value = "/user/gene", method = RequestMethod.GET)
    public Collection<UserGene> getGenes() {
        return userService.findCurrentUser().getUserGenes().values();
    }

    @RequestMapping(value = "/user/term", method = RequestMethod.GET)
    public Set<UserTerm> getTerms() {
        return userService.findCurrentUser().getUserTerms();
    }

    @RequestMapping(value = "/user/model/{taxonId}", method = RequestMethod.POST)
    public String saveModelForTaxon( @PathVariable Integer taxonId, @RequestBody Model model ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        user.getTaxonDescriptions().put( taxon, model.getDescription() );

        Map<Gene, TierType> genes = geneService.deserializeGenes( model.getGeneTierMap() );
        Set<GeneOntologyTerm> terms = model.getGoIds().stream().map( s -> goService.getTerm( s ) ).collect( Collectors.toSet() );

        userService.updateTermsAndGenesInTaxon( user, taxon, genes, terms );

        return "Saved.";
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/gene", method = RequestMethod.GET)
    public Set<UserGene> getGenesForTaxon( @PathVariable Integer taxonId ) {
        Taxon taxon = taxonService.findById( taxonId );
        return userService.findCurrentUser().getUserGenes().values().stream()
                .filter( gene -> gene.getTaxon().equals( taxon ) ).collect(Collectors.toSet());
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/term", method = RequestMethod.GET)
    public Set<UserTerm> getTermsForTaxon( @PathVariable Integer taxonId ) {
        Taxon taxon = taxonService.findById( taxonId );
        return userService.findCurrentUser().getTermsByTaxon( taxon );
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/term/search", method = RequestMethod.POST)
    public Map<String, UserTerm> getTermsForTaxon( @PathVariable Integer taxonId, @RequestBody List<String> goIds ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        return goIds.stream().collect( HashMap::new, ( m, s)->m.put(s, userService.convertTerms( user, taxon, goService.getTerm( s ) )), HashMap::putAll);
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/term/recommend", method = RequestMethod.GET)
    public Collection<UserTerm> getRecommendedTermsForTaxon( @PathVariable Integer taxonId, @RequestParam(value = "top", required = false, defaultValue = "false") boolean top ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );
        Collection<UserTerm> terms = userService.recommendTerms( user, taxon );

        // Remove terms already added
        terms.removeAll( user.getTermsByTaxon( taxon ) );
        ;
        if ( top ) {
            return terms.stream().collect(maxList(comparing(UserTerm::getFrequency)));
        }

        return terms;
    }

    static <T> Collector<T,?,List<T>> maxList( Comparator<? super T> comp) {
        return Collector.of(
                ArrayList::new,
                (list, t) -> {
                    int c;
                    if (list.isEmpty() || (c = comp.compare(t, list.get(0))) == 0) {
                        list.add(t);
                    } else if (c > 0) {
                        list.clear();
                        list.add(t);
                    }
                },
                (list1, list2) -> {
                    if (list1.isEmpty()) {
                        return list2;
                    }
                    if (list2.isEmpty()) {
                        return list1;
                    }
                    int r = comp.compare(list1.get(0), list2.get(0));
                    if (r < 0) {
                        return list2;
                    } else if (r > 0) {
                        return list1;
                    } else {
                        list1.addAll(list2);
                        return list1;
                    }
                });
    }

}
