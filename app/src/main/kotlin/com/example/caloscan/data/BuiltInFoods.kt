package com.example.caloscan.data

import com.example.caloscan.data.FoodCategory.DAIRY
import com.example.caloscan.data.FoodCategory.DRINK
import com.example.caloscan.data.FoodCategory.FAT
import com.example.caloscan.data.FoodCategory.FRUIT
import com.example.caloscan.data.FoodCategory.GRAIN
import com.example.caloscan.data.FoodCategory.LEGUME
import com.example.caloscan.data.FoodCategory.MEAL
import com.example.caloscan.data.FoodCategory.MEAT
import com.example.caloscan.data.FoodCategory.SNACK
import com.example.caloscan.data.FoodCategory.VEG
import com.example.caloscan.data.model.Food
import com.example.caloscan.data.model.FoodSource
import com.example.caloscan.data.model.Serving
import java.text.Normalizer

enum class FoodCategory(val id: String, val label: String, val emoji: String) {
  FRUIT("fruit", "Фрукты", "🍎"),
  VEG("veg", "Овощи", "🥦"),
  GRAIN("grain", "Крупы и хлеб", "🍞"),
  MEAT("meat", "Мясо и рыба", "🍗"),
  DAIRY("dairy", "Яйца и молочные", "🥚"),
  LEGUME("legume", "Бобовые и орехи", "🥜"),
  SNACK("snack", "Снеки и сладости", "🍫"),
  DRINK("drink", "Напитки", "🥤"),
  MEAL("meal", "Блюда и фастфуд", "🍽️"),
  FAT("fat", "Масла, соусы, сахар", "🥄"),
}

/**
 * Built-in food database.
 *
 * All nutrition values are per 100 g (or per 100 ml for drinks) and are
 * rounded typical values based on the USDA FoodData Central database.
 * Real products vary, so packaged foods should be scanned instead.
 *
 * Arguments of food(): English name (used for the id and as an extra search word),
 * Russian name, category, kcal, protein, carbs, fat (all per 100 g),
 * serving name, serving size in grams (or ml), emoji, unit.
 */
object BuiltInFoods {

  val all: List<Food> = listOf(
    // Фрукты
    food("Apple", "Яблоко", FRUIT, 52, 0.3, 13.8, 0.2, "1 шт.", 182, "🍎"),
    food("Banana", "Банан", FRUIT, 89, 1.1, 22.8, 0.3, "1 шт.", 118, "🍌"),
    food("Orange", "Апельсин", FRUIT, 47, 0.9, 11.8, 0.1, "1 шт.", 131, "🍊"),
    food("Pear", "Груша", FRUIT, 57, 0.4, 15.2, 0.1, "1 шт.", 178, "🍐"),
    food("Grapes", "Виноград", FRUIT, 69, 0.7, 18.1, 0.2, "1 стакан", 151, "🍇"),
    food("Strawberries", "Клубника", FRUIT, 32, 0.7, 7.7, 0.3, "1 стакан", 152, "🍓"),
    food("Blueberries", "Черника", FRUIT, 57, 0.7, 14.5, 0.3, "1 стакан", 148, "🫐"),
    food("Raspberries", "Малина", FRUIT, 52, 1.2, 11.9, 0.7, "1 стакан", 123),
    food("Watermelon", "Арбуз", FRUIT, 30, 0.6, 7.6, 0.2, "1 ломоть", 286, "🍉"),
    food("Melon (cantaloupe)", "Дыня", FRUIT, 34, 0.8, 8.2, 0.2, "1 стакан", 160, "🍈"),
    food("Pineapple", "Ананас", FRUIT, 50, 0.5, 13.1, 0.1, "1 стакан", 165, "🍍"),
    food("Mango", "Манго", FRUIT, 60, 0.8, 15.0, 0.4, "1 стакан", 165, "🥭"),
    food("Kiwi", "Киви", FRUIT, 61, 1.1, 14.7, 0.5, "1 шт.", 69, "🥝"),
    food("Peach", "Персик", FRUIT, 39, 0.9, 9.5, 0.3, "1 шт.", 150, "🍑"),
    food("Plum", "Слива", FRUIT, 46, 0.7, 11.4, 0.3, "1 шт.", 66),
    food("Cherries", "Черешня", FRUIT, 63, 1.1, 16.0, 0.2, "1 стакан", 138, "🍒"),
    food("Avocado", "Авокадо", FRUIT, 160, 2.0, 8.5, 14.7, "½ авокадо", 75, "🥑"),
    food("Lemon", "Лимон", FRUIT, 29, 1.1, 9.3, 0.3, "1 шт.", 58, "🍋"),
    food("Dates, dried", "Финики сушёные", FRUIT, 282, 2.5, 75.0, 0.4, "1 шт.", 8),
    food("Raisins", "Изюм", FRUIT, 299, 3.1, 79.2, 0.5, "1 горсть", 30),
    // Овощи
    food("Broccoli", "Брокколи", VEG, 34, 2.8, 6.6, 0.4, "1 стакан", 91, "🥦"),
    food("Carrot", "Морковь", VEG, 41, 0.9, 9.6, 0.2, "1 шт.", 61, "🥕"),
    food("Tomato", "Помидор", VEG, 18, 0.9, 3.9, 0.2, "1 шт.", 123, "🍅"),
    food("Cucumber", "Огурец", VEG, 15, 0.7, 3.6, 0.1, "½ огурца", 150, "🥒"),
    food("Lettuce", "Салат листовой", VEG, 15, 1.4, 2.9, 0.2, "1 стакан", 36, "🥬"),
    food("Spinach", "Шпинат", VEG, 23, 2.9, 3.6, 0.4, "1 стакан", 30, "🥬"),
    food("Bell pepper", "Перец болгарский", VEG, 31, 1.0, 6.0, 0.3, "1 шт.", 119, "🫑"),
    food("Onion", "Лук репчатый", VEG, 40, 1.1, 9.3, 0.1, "1 шт.", 110, "🧅"),
    food("Garlic", "Чеснок", VEG, 149, 6.4, 33.1, 0.5, "1 зубчик", 3, "🧄"),
    food("Potato, boiled", "Картофель отварной", VEG, 87, 1.9, 20.1, 0.1, "1 шт.", 170, "🥔"),
    food("Mashed potatoes", "Картофельное пюре", VEG, 113, 1.9, 16.9, 4.2, "1 стакан", 210, "🥔"),
    food("Sweet potato, baked", "Батат запечённый", VEG, 90, 2.0, 20.7, 0.2, "1 шт.", 114, "🍠"),
    food("Cauliflower", "Цветная капуста", VEG, 25, 1.9, 5.0, 0.3, "1 стакан", 107),
    food("Zucchini", "Кабачок", VEG, 17, 1.2, 3.1, 0.3, "1 шт.", 196),
    food("Mushrooms", "Шампиньоны", VEG, 22, 3.1, 3.3, 0.3, "1 стакан", 70, "🍄"),
    food("Green peas", "Зелёный горошек", VEG, 81, 5.4, 14.5, 0.4, "½ стакана", 73, "🫛"),
    food("Sweet corn, boiled", "Кукуруза отварная", VEG, 96, 3.4, 21.0, 1.5, "1 початок", 100, "🌽"),
    food("Cabbage", "Капуста белокочанная", VEG, 25, 1.3, 5.8, 0.1, "1 стакан", 89),
    food("Green beans", "Стручковая фасоль", VEG, 31, 1.8, 7.0, 0.2, "1 стакан", 100),
    food("Eggplant", "Баклажан", VEG, 25, 1.0, 5.9, 0.2, "1 стакан", 82, "🍆"),
    food("Beetroot", "Свёкла", VEG, 43, 1.6, 9.6, 0.2, "1 шт.", 82),
    // Крупы и хлеб
    food("White rice, cooked", "Рис белый варёный", GRAIN, 130, 2.7, 28.2, 0.3, "1 стакан", 158, "🍚"),
    food("Brown rice, cooked", "Рис бурый варёный", GRAIN, 123, 2.7, 25.6, 1.0, "1 стакан", 195, "🍚"),
    food("Pasta, cooked", "Макароны варёные", GRAIN, 158, 5.8, 30.9, 0.9, "1 стакан", 140, "🍝"),
    food("Spaghetti, dry", "Спагетти сухие", GRAIN, 371, 13.0, 74.7, 1.5, "1 порция", 80, "🍝"),
    food("Oats, rolled (dry)", "Овсяные хлопья (сухие)", GRAIN, 379, 13.2, 67.7, 6.5, "½ стакана", 40, "🥣"),
    food("Oatmeal, cooked with water", "Овсяная каша на воде", GRAIN, 71, 2.5, 12.0, 1.5, "1 тарелка", 234, "🥣"),
    food("Buckwheat, cooked", "Гречка варёная", GRAIN, 92, 3.4, 19.9, 0.6, "1 стакан", 168),
    food("Quinoa, cooked", "Киноа варёная", GRAIN, 120, 4.4, 21.3, 1.9, "1 стакан", 185),
    food("Couscous, cooked", "Кускус варёный", GRAIN, 112, 3.8, 23.2, 0.2, "1 стакан", 157),
    food("White bread", "Хлеб белый", GRAIN, 265, 9.0, 49.0, 3.2, "1 ломтик", 28, "🍞"),
    food("Whole wheat bread", "Хлеб цельнозерновой", GRAIN, 247, 13.0, 41.0, 3.4, "1 ломтик", 32, "🍞"),
    food("Rye bread", "Хлеб ржаной", GRAIN, 259, 8.5, 48.3, 3.3, "1 ломтик", 32, "🍞"),
    food("Bagel", "Бейгл", GRAIN, 257, 10.0, 50.0, 1.6, "1 шт.", 105, "🥯"),
    food("Croissant", "Круассан", GRAIN, 406, 8.2, 45.8, 21.0, "1 шт.", 57, "🥐"),
    food("Flour tortilla", "Тортилья пшеничная", GRAIN, 312, 8.3, 51.6, 8.0, "1 шт.", 45, "🫓"),
    food("Corn flakes", "Кукурузные хлопья", GRAIN, 357, 7.5, 84.0, 0.4, "1 стакан", 28, "🥣"),
    food("Granola", "Гранола", GRAIN, 471, 10.0, 64.0, 20.0, "½ стакана", 60, "🥣"),
    food("Pancakes", "Панкейки", GRAIN, 227, 6.4, 28.3, 9.7, "1 шт.", 40, "🥞"),
    food("Wheat flour", "Мука пшеничная", GRAIN, 364, 10.3, 76.3, 1.0, "1 стакан", 125),
    // Мясо и рыба
    food("Chicken breast, cooked", "Куриная грудка варёная", MEAT, 165, 31.0, 0, 3.6, "1 филе", 120, "🍗"),
    food("Chicken thigh, cooked", "Куриное бедро варёное", MEAT, 209, 26.0, 0, 10.9, "1 шт.", 100, "🍗"),
    food("Chicken wings, roasted", "Куриные крылья запечённые", MEAT, 254, 23.8, 0, 16.9, "1 шт.", 34, "🍗"),
    food("Ground beef (85% lean), cooked", "Говяжий фарш (15% жира), жареный", MEAT, 250, 25.9, 0, 15.4, "1 котлета", 85, "🥩"),
    food("Beef steak, grilled", "Стейк говяжий на гриле", MEAT, 217, 26.1, 0, 11.8, "1 стейк", 150, "🥩"),
    food("Pork chop, cooked", "Свиная отбивная", MEAT, 231, 25.7, 0, 13.9, "1 шт.", 150, "🥩"),
    food("Bacon, cooked", "Бекон жареный", MEAT, 541, 37.0, 1.4, 42.0, "1 ломтик", 8, "🥓"),
    food("Ham, sliced", "Ветчина", MEAT, 145, 21.0, 1.5, 5.5, "1 ломтик", 28),
    food("Turkey breast, roasted", "Грудка индейки запечённая", MEAT, 147, 30.1, 0, 2.1, "3 ломтика", 85),
    food("Pork sausage, cooked", "Колбаски свиные жареные", MEAT, 301, 19.4, 2.0, 24.0, "1 шт.", 45, "🌭"),
    food("Frankfurter", "Сосиска", MEAT, 290, 10.3, 4.2, 26.0, "1 шт.", 45, "🌭"),
    food("Salmon, cooked", "Лосось приготовленный", MEAT, 206, 22.1, 0, 12.4, "1 филе", 150, "🐟"),
    food("Tuna, canned in water", "Тунец в собственном соку", MEAT, 116, 25.5, 0, 0.8, "1 банка (без жидкости)", 112, "🐟"),
    food("Cod, cooked", "Треска приготовленная", MEAT, 105, 22.8, 0, 0.9, "1 филе", 150, "🐟"),
    food("Shrimp, cooked", "Креветки варёные", MEAT, 99, 24.0, 0.2, 0.3, "1 порция", 85, "🍤"),
    // Яйца и молочные продукты
    food("Egg, boiled", "Яйцо варёное", DAIRY, 155, 12.6, 1.1, 10.6, "1 крупное", 50, "🥚"),
    food("Egg, fried", "Яйцо жареное", DAIRY, 196, 13.6, 0.8, 14.8, "1 крупное", 46, "🍳"),
    food("Egg white", "Яичный белок", DAIRY, 52, 10.9, 0.7, 0.2, "из 1 яйца", 33, "🥚"),
    food("Milk, whole (3.3%)", "Молоко 3,3%", DAIRY, 61, 3.2, 4.8, 3.3, "1 стакан", 250, "🥛", unit = "ml"),
    food("Milk, 2%", "Молоко 2%", DAIRY, 50, 3.3, 4.8, 2.0, "1 стакан", 250, "🥛", unit = "ml"),
    food("Milk, skim", "Молоко обезжиренное", DAIRY, 34, 3.4, 5.0, 0.1, "1 стакан", 250, "🥛", unit = "ml"),
    food("Kefir, low-fat", "Кефир 1%", DAIRY, 41, 3.8, 4.5, 0.9, "1 стакан", 250, "🥛", unit = "ml"),
    food("Greek yogurt, plain 0%", "Греческий йогурт 0%", DAIRY, 59, 10.2, 3.6, 0.4, "1 баночка", 170),
    food("Yogurt, plain whole milk", "Йогурт натуральный", DAIRY, 61, 3.5, 4.7, 3.3, "1 стакан", 245),
    food("Cottage cheese, 2%", "Творог 2%", DAIRY, 84, 11.0, 4.3, 2.3, "½ стакана", 113),
    food("Cheddar cheese", "Сыр чеддер", DAIRY, 403, 24.9, 1.3, 33.1, "1 ломтик", 28, "🧀"),
    food("Mozzarella", "Моцарелла", DAIRY, 300, 22.2, 2.2, 22.4, "1 ломтик", 28, "🧀"),
    food("Parmesan, grated", "Пармезан тёртый", DAIRY, 392, 35.8, 3.2, 25.8, "1 ст. л.", 5, "🧀"),
    food("Feta cheese", "Сыр фета", DAIRY, 264, 14.2, 4.1, 21.3, "1 ломтик", 28, "🧀"),
    food("Cream cheese", "Сливочный сыр", DAIRY, 342, 5.9, 4.1, 34.2, "1 ст. л.", 15),
    food("Sour cream", "Сметана", DAIRY, 198, 2.4, 4.6, 19.4, "1 ст. л.", 12),
    food("Whey protein powder", "Сывороточный протеин", DAIRY, 400, 80.0, 8.0, 6.0, "1 мерная ложка", 30),
    // Бобовые, орехи и семена
    food("Lentils, cooked", "Чечевица варёная", LEGUME, 116, 9.0, 20.1, 0.4, "1 стакан", 198),
    food("Chickpeas, cooked", "Нут варёный", LEGUME, 164, 8.9, 27.4, 2.6, "1 стакан", 164),
    food("Black beans, cooked", "Фасоль чёрная варёная", LEGUME, 132, 8.9, 23.7, 0.5, "1 стакан", 172, "🫘"),
    food("Kidney beans, cooked", "Фасоль красная варёная", LEGUME, 127, 8.7, 22.8, 0.5, "1 стакан", 177, "🫘"),
    food("Edamame", "Эдамаме", LEGUME, 121, 11.9, 8.9, 5.2, "1 стакан", 155, "🫛"),
    food("Tofu, firm", "Тофу твёрдый", LEGUME, 144, 17.3, 2.8, 8.7, "1 порция", 100),
    food("Hummus", "Хумус", LEGUME, 166, 7.9, 14.3, 9.6, "2 ст. л.", 30),
    food("Almonds", "Миндаль", LEGUME, 579, 21.2, 21.6, 49.9, "1 горсть", 28),
    food("Peanuts", "Арахис", LEGUME, 567, 25.8, 16.1, 49.2, "1 горсть", 28, "🥜"),
    food("Walnuts", "Грецкие орехи", LEGUME, 654, 15.2, 13.7, 65.2, "1 горсть", 28),
    food("Cashews", "Кешью", LEGUME, 553, 18.2, 30.2, 43.9, "1 горсть", 28),
    food("Peanut butter", "Арахисовая паста", LEGUME, 588, 25.1, 20.0, 50.4, "1 ст. л.", 16, "🥜"),
    food("Sunflower seeds", "Семечки подсолнечника", LEGUME, 584, 20.8, 20.0, 51.5, "¼ стакана", 35, "🌻"),
    food("Chia seeds", "Семена чиа", LEGUME, 486, 16.5, 42.1, 30.7, "1 ст. л.", 12),
    // Снеки и сладости
    food("Dark chocolate (70–85%)", "Тёмный шоколад (70–85%)", SNACK, 598, 7.8, 45.9, 42.6, "1 долька", 10, "🍫"),
    food("Milk chocolate", "Молочный шоколад", SNACK, 535, 7.7, 59.4, 29.7, "1 плитка", 43, "🍫"),
    food("Potato chips", "Картофельные чипсы", SNACK, 536, 7.0, 53.0, 35.0, "1 маленькая пачка", 28),
    food("Popcorn, air-popped", "Попкорн без масла", SNACK, 387, 12.9, 77.8, 4.5, "1 стакан", 8, "🍿"),
    food("Pretzels", "Солёные крендельки", SNACK, 380, 10.3, 79.8, 2.9, "1 горсть", 28, "🥨"),
    food("Ice cream, vanilla", "Мороженое ванильное", SNACK, 207, 3.5, 23.6, 11.0, "1 шарик", 66, "🍨"),
    food("Chocolate chip cookie", "Печенье с шоколадной крошкой", SNACK, 488, 5.4, 64.0, 24.0, "1 шт.", 16, "🍪"),
    food("Glazed donut", "Пончик в глазури", SNACK, 403, 4.9, 45.0, 22.0, "1 шт.", 60, "🍩"),
    food("Chocolate cake", "Шоколадный торт", SNACK, 367, 4.1, 54.6, 16.4, "1 кусок", 80, "🍰"),
    food("Gummy candy", "Жевательный мармелад", SNACK, 343, 6.9, 77.0, 0.2, "1 горсть", 30, "🍬"),
    // Напитки (на 100 мл)
    food("Water", "Вода", DRINK, 0, 0, 0, 0, "1 стакан", 250, "💧"),
    food("Coffee, black", "Кофе чёрный", DRINK, 2, 0.3, 0, 0, "1 чашка", 240, "☕"),
    food("Tea, unsweetened", "Чай без сахара", DRINK, 1, 0, 0.3, 0, "1 чашка", 240, "🍵"),
    food("Latte (2% milk)", "Латте (молоко 2%)", DRINK, 40, 2.6, 3.9, 1.6, "1 чашка", 350, "☕"),
    food("Orange juice", "Сок апельсиновый", DRINK, 45, 0.7, 10.4, 0.2, "1 стакан", 250, "🧃"),
    food("Apple juice", "Сок яблочный", DRINK, 46, 0.1, 11.3, 0.1, "1 стакан", 250, "🧃"),
    food("Cola", "Кола", DRINK, 42, 0, 10.6, 0, "1 банка", 330, "🥤"),
    food("Cola, diet / zero", "Кола без сахара", DRINK, 0, 0, 0, 0, "1 банка", 330, "🥤"),
    food("Energy drink", "Энергетик", DRINK, 45, 0, 11.0, 0, "1 банка", 250, "🥤"),
    food("Beer", "Пиво", DRINK, 43, 0.5, 3.6, 0, "1 бутылка", 330, "🍺"),
    food("Red wine", "Вино красное", DRINK, 85, 0.1, 2.6, 0, "1 бокал", 150, "🍷"),
    // Блюда и фастфуд
    food("Pizza, cheese", "Пицца с сыром", MEAL, 266, 11.4, 33.3, 9.7, "1 кусок", 107, "🍕"),
    food("Hamburger", "Гамбургер", MEAL, 254, 13.0, 26.0, 11.0, "1 шт.", 150, "🍔"),
    food("French fries", "Картофель фри", MEAL, 312, 3.4, 41.4, 14.7, "1 средняя порция", 117, "🍟"),
    food("Hot dog in a bun", "Хот-дог", MEAL, 247, 10.6, 18.4, 14.8, "1 шт.", 98, "🌭"),
    food("Chicken nuggets", "Куриные наггетсы", MEAL, 296, 15.3, 14.0, 19.8, "6 шт.", 96),
    food("Sushi (California roll)", "Ролл «Калифорния»", MEAL, 140, 4.5, 22.0, 3.7, "1 шт.", 30, "🍣"),
    food("Caesar salad with chicken", "Салат «Цезарь» с курицей", MEAL, 127, 9.5, 5.0, 7.7, "1 тарелка", 300, "🥗"),
    food("Burrito, beef & bean", "Буррито с говядиной и фасолью", MEAL, 210, 9.0, 25.0, 8.5, "1 шт.", 220, "🌯"),
    food("Spaghetti bolognese", "Спагетти болоньезе", MEAL, 132, 6.5, 16.0, 4.5, "1 тарелка", 350, "🍝"),
    food("Chicken noodle soup", "Куриный суп с лапшой", MEAL, 36, 2.6, 4.4, 1.0, "1 тарелка", 250, "🍜"),
    food("Omelette (plain)", "Омлет", MEAL, 154, 10.6, 0.6, 11.7, "из 2 яиц", 120, "🍳"),
    food("Ham & cheese sandwich", "Сэндвич с ветчиной и сыром", MEAL, 241, 14.0, 26.0, 9.0, "1 шт.", 150, "🥪"),
    // Масла, соусы и сахар
    food("Olive oil", "Оливковое масло", FAT, 884, 0, 0, 100, "1 ст. л.", 14, "🫒"),
    food("Sunflower oil", "Подсолнечное масло", FAT, 884, 0, 0, 100, "1 ст. л.", 14),
    food("Butter", "Сливочное масло", FAT, 717, 0.9, 0.1, 81.1, "1 ст. л.", 14, "🧈"),
    food("Mayonnaise", "Майонез", FAT, 680, 1.0, 0.6, 75.0, "1 ст. л.", 14),
    food("Ketchup", "Кетчуп", FAT, 101, 1.0, 27.4, 0.1, "1 ст. л.", 17),
    food("Soy sauce", "Соевый соус", FAT, 53, 8.1, 4.9, 0.6, "1 ст. л.", 16),
    food("Sugar", "Сахар", FAT, 387, 0, 100, 0, "1 ч. л.", 4),
    food("Honey", "Мёд", FAT, 304, 0.3, 82.4, 0, "1 ст. л.", 21, "🍯"),
    food("Jam", "Варенье", FAT, 278, 0.4, 68.9, 0.1, "1 ст. л.", 20),
    food("Chocolate hazelnut spread", "Шоколадно-ореховая паста", FAT, 539, 6.3, 57.5, 30.9, "1 ст. л.", 19),
  )

  private val byId: Map<String, Food> = all.associateBy { it.id }

  fun get(id: String): Food? = byId[id]

  fun inCategory(category: FoodCategory): List<Food> = all.filter { it.category == category.id }
}

private fun food(
  englishName: String,
  name: String,
  category: FoodCategory,
  kcal: Number,
  protein: Number,
  carbs: Number,
  fat: Number,
  servingLabel: String,
  servingSize: Number,
  emoji: String? = null,
  unit: String? = null,
) = Food(
  // The id comes from the English name, so it stays the same if the Russian name changes
  id = "b-" + slug(englishName),
  name = name,
  searchTerms = englishName,
  kcal = kcal.toDouble(),
  protein = protein.toDouble(),
  carbs = carbs.toDouble(),
  fat = fat.toDouble(),
  source = FoodSource.BUILTIN,
  category = category.id,
  serving = Serving(servingLabel, servingSize.toDouble()),
  unit = unit ?: if (category == DRINK) "ml" else "g",
  emoji = emoji ?: category.emoji,
)

/** "Oats, rolled (dry)" → "oats-rolled-dry". Used to build stable food ids. */
internal fun slug(text: String): String =
  Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')
