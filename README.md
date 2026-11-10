# ErethonSkills
Maaaagic!

* %skill_cooldown%
* %skill_range%
* %skill_duration%
* %skill_radius%
* %skill_energy%

* %attribute_<name>%

## Optional Hecate attribute curves

Add `attributeCurves` to a class file under `plugins/Hecate/classes/` to test
the `Skalierungen` progression formula over character levels 1–20. Each curve
sets that attribute's base before equipment and effects. Attributes without a
curve use the discipline's `characterLevels` bonuses. Restart the server to
test class configuration changes with existing characters.

```yaml
attributeCurves:
  "minecraft:advantage_physical":
    base: 7.1
    multiplier: 1.0
    growth: 0.045
    exponent: 1.15
```

The value is `base × multiplier × (1 + growth × (level − 1))^exponent`.
The optional `multiplier` defaults to `1.0`.
