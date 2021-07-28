package recipes.control;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import recipes.model.Recipe;
import recipes.model.UserDetailsImpl;
import recipes.repository.RecipeRepository;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "api")
@Validated
public class RecipeController {
    private final RecipeRepository recipeRepository;

    @Autowired
    public RecipeController(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @GetMapping("/recipe/{id}")
    public Recipe getRecipe(@PathVariable Long id) {
        Optional<Recipe> result = recipeRepository.findById(id);
        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "recipe not found");
        }
        return result.get();
    }

    @GetMapping(value = "/recipe/search", params = "category")
    public List<Recipe> searchRecipesByCategory (String category) {
        return recipeRepository.findByCategoryIgnoreCaseOrderByDateDesc(category);
    }

    @GetMapping(value = "/recipe/search", params = "name")
    public List<Recipe> searchRecipesByName (String name) {
        return recipeRepository.findByNameContainingIgnoreCaseOrderByDateDesc(name);
    }

    @DeleteMapping("/recipe/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl user) {
        Recipe entity = getRecipeAndValidateUser(id, user);
        recipeRepository.delete(entity);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/recipe/new")
    public Map<String, Long> createRecipe(@RequestBody @Valid Recipe recipe, @AuthenticationPrincipal UserDetailsImpl user) {
        recipe.setDate(LocalDateTime.now());
        recipe.setUserEmail(user.getUsername());
        Recipe savedVersion = recipeRepository.save(recipe);
        return Collections.singletonMap("id", savedVersion.getId());
    }

    @PutMapping("/recipe/{id}")
    public ResponseEntity<Void> putRecipe(@PathVariable Long id, @RequestBody @Valid Recipe recipe, @AuthenticationPrincipal UserDetailsImpl user) {
        Recipe existing = getRecipeAndValidateUser(id, user);
        existing.update(recipe);
        recipeRepository.save(existing);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private Recipe getRecipeAndValidateUser(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl user) {
        Optional<Recipe> result = recipeRepository.findById(id);
        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "recipe not found");
        }
        Recipe existing = result.get();
        if (!existing.getUserEmail().equals(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return existing;
    }
}
