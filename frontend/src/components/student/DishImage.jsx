import { useState } from 'react';
import { ASSET_PATHS } from '../../utils/constants';

/** Dish photo with graceful fallback to the placeholder if the file is missing. */
export default function DishImage({ imageFile, name, className = 'h-20 w-20 sm:h-24 sm:w-28' }) {
  const [failed, setFailed] = useState(false);
  return (
    <img src={failed ? ASSET_PATHS.dishPlaceholder : ASSET_PATHS.dishImage(imageFile)} alt={name ?? ''}
      onError={() => setFailed(true)} className={`${className} shrink-0 bg-[#FFF5F3] object-cover`} />
  );
}
