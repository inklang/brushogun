// ink.mobs grammar extension
// Mob grammar with equipment, drops, and skills support.
// Meta fields (name:, health:, damage:) are handled by the base Ink grammar.
import { defineGrammar, declaration, rule, keyword, identifier, int, float, string } from '@inklang/quill/grammar'

export default defineGrammar({
  package: 'ink.mobs',
  declarations: [
    declaration({
      keyword: 'mob',
      inheritsBase: true,
      rules: [
        // equipment { head: DIAMOND_HELMET, main: IRON_SWORD, ... }
        rule('equipment_slot', r => r.seq(
          r.choice([
            r.keyword('head'), r.keyword('main'), r.keyword('offhand'),
            r.keyword('legs'), r.keyword('chest'),
            r.keyword('helmet'), r.keyword('chestplate'), r.keyword('leggings'),
            r.keyword('boots')
          ]),
          r.literal(':'),
          identifier()
        )),
        rule('equipment_block', r => r.seq(
          r.keyword('equipment'),
          r.block(r.many1(r.equipment_slot()))
        )),

        // drops { IRON_INGOT 50, DIAMOND 5 1 }
        // Format: <item_identifier> [chance_identifier] [amount_identifier]
        // Each is optional and parsed as identifier tokens
        rule('drop_entry', r => r.seq(
          identifier(),                     // item name
          r.optional(identifier()),        // chance (optional)
          r.optional(identifier())         // amount (optional)
        )),
        rule('drops_block', r => r.seq(
          r.keyword('drops'),
          r.block(r.many1(r.drop_entry()))
        )),

        // experience: 100
        rule('experience_field', r => r.seq(
          r.keyword('experience'),
          r.literal(':'),
          int()
        )),

        // skills { on_spawn { ... }, on_damaged { ... } }
        // Threshold format: on_damaged gt 50 — threshold args parsed as identifiers
        rule('trigger_threshold', r => r.seq(
          identifier(),  // op as identifier e.g. "gt", "lt", "eq"
          int()          // threshold value
        )),
        rule('skill_event', r => r.choice(
          r.seq(r.keyword('on_spawn'),       r.optional(r.trigger_threshold())),
          r.seq(r.keyword('on_death'),       r.optional(r.trigger_threshold())),
          r.seq(r.keyword('on_damage'),      r.optional(r.trigger_threshold())),
          r.seq(r.keyword('on_damaged'),    r.optional(r.trigger_threshold())),
          r.seq(r.keyword('on_explode'),   r.optional(r.trigger_threshold())),
          r.seq(r.keyword('on_target'),     r.optional(r.trigger_threshold())),
          r.seq(r.keyword('on_interact'),   r.optional(r.trigger_threshold())),
          r.seq(r.keyword('on_enter_combat'), r.optional(r.trigger_threshold())),
          r.seq(r.keyword('on_leave_combat'), r.optional(r.trigger_threshold())),
          r.seq(r.keyword('on_tick'),       r.optional(r.keyword('every')), int())
        )),
        rule('skill_call', r => r.seq(
          identifier(),
          r.many(identifier())
        )),
        rule('skills_block', r => r.seq(
          r.keyword('skills'),
          r.block(r.many(r.seq(r.skill_event(), r.block(r.many1(r.skill_call())))))
        )),
      ]
    })
  ]
})
