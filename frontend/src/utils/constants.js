export const MEAL_TYPES = ['BREAKFAST', 'LUNCH', 'SNACKS', 'DINNER'];

export const MEAL_LABELS = { BREAKFAST: 'Breakfast', LUNCH: 'Lunch', SNACKS: 'Snacks', DINNER: 'Dinner' };

export const RESPONSE_STATUS = { ATTENDING: 'ATTENDING', NOT_ATTENDING: 'NOT_ATTENDING', NOT_SURE: 'NOT_SURE' };

export const RESPONSE_LABELS = {
  ATTENDING: "I'll be there",
  NOT_ATTENDING: "I won't be there",
  NOT_SURE: 'Not sure yet',
};

export const ROLES = { STUDENT: 'STUDENT', ADMIN: 'ADMIN' };

export const HISTORY_PERIODS = [
  { value: 'TODAY', label: "Today's selections" },
  { value: 'LAST_WEEK', label: "Last week's selections" },
  { value: 'LAST_MONTH', label: "Last month's selections" },
];

export const ANALYTICS_SCOPES = [
  { value: 'THIS_MONTH', label: 'This month' },
  { value: 'ALL_TIME', label: 'All time' },
];

// Single source of truth for every static asset. If an exported file has a
// different extension (e.g. logo .png instead of .svg) change it here only.
export const ASSET_PATHS = {
  logo: '/assets/brand/preplate-logo.svg',
  favicon: '/assets/brand/preplate-favicon.png',
  icons: {
    home: '/assets/icons/home.svg',
    foodHistory: '/assets/icons/food-history.svg',
    logout: '/assets/icons/logout.svg',
    bell: '/assets/icons/bell.svg',
    profile: '/assets/icons/profile.svg',
    plate: '/assets/icons/plate.svg',
    adminDashboard: '/assets/icons/admin-dashboard.svg',
    adminMenu: '/assets/icons/admin-menu.svg',
    adminWaste: '/assets/icons/admin-waste.svg',
  },
  mealImages: {
    BREAKFAST: '/assets/meals/breakfast.jpg',
    LUNCH: '/assets/meals/lunch.jpg',
    SNACKS: '/assets/meals/snacks.jpg',
    DINNER: '/assets/meals/dinner.jpg',
  },
  dishPlaceholder: '/assets/dishes/dish-placeholder.png',
  dishImage: (imageFile) => (imageFile ? `/assets/dishes/${imageFile}` : '/assets/dishes/dish-placeholder.png'),
};

export const FALLBACK_FACTS = [
  'Every response helps the kitchen prepare closer to actual demand.',
  'Cooking only what is needed saves food, fuel, water and money.',
  'Small habits, like confirming your meal on time, add up to big reductions in food waste.',
];
